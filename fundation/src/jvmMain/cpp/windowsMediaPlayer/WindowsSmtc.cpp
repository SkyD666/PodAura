#include "WindowsMediaSession.h"

#include <systemmediatransportcontrolsinterop.h>

#include <winrt/Windows.Foundation.h>

#include <algorithm>
#include <cmath>
#include <cstdint>

namespace podaura::windows_media {
namespace {

using winrt::Windows::Foundation::TimeSpan;
using winrt::Windows::Media::MediaPlaybackStatus;
using winrt::Windows::Media::MediaPlaybackType;
using winrt::Windows::Media::PlaybackPositionChangeRequestedEventArgs;
using winrt::Windows::Media::SystemMediaTransportControls;
using winrt::Windows::Media::SystemMediaTransportControlsButton;
using winrt::Windows::Media::SystemMediaTransportControlsButtonPressedEventArgs;
using winrt::Windows::Media::SystemMediaTransportControlsTimelineProperties;
using winrt::Windows::Storage::Streams::DataWriter;
using winrt::Windows::Storage::Streams::InMemoryRandomAccessStream;
using winrt::Windows::Storage::Streams::RandomAccessStreamReference;

constexpr int64_t kTicksPerSecond = 10'000'000LL;

TimeSpan seconds_to_timespan(double seconds) {
    const auto safe_seconds = std::isfinite(seconds) ? std::max(0.0, seconds) : 0.0;
    return TimeSpan{static_cast<int64_t>(safe_seconds * kTicksPerSecond)};
}

} // namespace

void Session::initialize_smtc(HWND window) {
    if (smtc_) return;
    ensure_unpacked_start_menu_shortcut();
    SystemMediaTransportControls controls{nullptr};
    auto interop = winrt::get_activation_factory<
            SystemMediaTransportControls,
            ISystemMediaTransportControlsInterop>();
    winrt::check_hresult(interop->GetForWindow(
            window,
            winrt::guid_of<SystemMediaTransportControls>(),
            winrt::put_abi(controls)
    ));
    const auto weak = weak_from_this();
    winrt::event_token button_token{};
    winrt::event_token position_token{};
    bool button_attached = false;
    bool position_attached = false;
    try {
        button_token = controls.ButtonPressed(
                [weak](const SystemMediaTransportControls &,
                       const SystemMediaTransportControlsButtonPressedEventArgs &args) {
                    if (auto session = weak.lock()) session->on_smtc_button(args.Button());
                }
        );
        button_attached = true;
        position_token = controls.PlaybackPositionChangeRequested(
                [weak](const SystemMediaTransportControls &,
                       const PlaybackPositionChangeRequestedEventArgs &args) {
                    if (auto session = weak.lock()) {
                        session->queue_command(
                                PODAURA_MEDIA_COMMAND_CHANGE_PLAYBACK_POSITION,
                                static_cast<double>(args.RequestedPlaybackPosition().count()) /
                                kTicksPerSecond
                        );
                    }
                }
        );
        position_attached = true;
        controls.IsEnabled(false);
    } catch (...) {
        if (position_attached) {
            try {
                controls.PlaybackPositionChangeRequested(position_token);
            } catch (...) {
            }
        }
        if (button_attached) {
            try {
                controls.ButtonPressed(button_token);
            } catch (...) {
            }
        }
        throw;
    }

    smtc_ = controls;
    button_token_ = button_token;
    position_token_ = position_token;
    smtc_events_attached_ = true;
}

void Session::release_smtc_locked() noexcept {
    if (!smtc_) return;
    auto controls = smtc_;
    smtc_ = nullptr;
    if (smtc_events_attached_) {
        try {
            controls.ButtonPressed(button_token_);
        } catch (...) {
        }
        try {
            controls.PlaybackPositionChangeRequested(position_token_);
        } catch (...) {
        }
    }
    smtc_events_attached_ = false;
    artwork_reference_ = nullptr;
    artwork_id_.clear();
}

void Session::on_smtc_button(SystemMediaTransportControlsButton button) {
    switch (button) {
        case SystemMediaTransportControlsButton::Play:
            queue_command(PODAURA_MEDIA_COMMAND_PLAY, 0.0);
            break;
        case SystemMediaTransportControlsButton::Pause:
            queue_command(PODAURA_MEDIA_COMMAND_PAUSE, 0.0);
            break;
        case SystemMediaTransportControlsButton::Previous:
            queue_command(PODAURA_MEDIA_COMMAND_PREVIOUS, 0.0);
            break;
        case SystemMediaTransportControlsButton::Next:
            queue_command(PODAURA_MEDIA_COMMAND_NEXT, 0.0);
            break;
        default:
            break;
    }
}

RandomAccessStreamReference Session::create_artwork_reference() const {
    if (info_.artwork_bytes.empty()) return nullptr;
    InMemoryRandomAccessStream stream;
    DataWriter writer(stream.GetOutputStreamAt(0));
    writer.WriteBytes(winrt::array_view<const uint8_t>(info_.artwork_bytes));
    writer.StoreAsync().get();
    writer.DetachStream();
    stream.Seek(0);
    return RandomAccessStreamReference::CreateFromStream(stream);
}

void Session::apply_smtc_locked() {
    if (!smtc_ || !has_media_) return;
    smtc_.IsEnabled(true);
    smtc_.IsPlayEnabled(availability_.can_play);
    smtc_.IsPauseEnabled(availability_.can_pause);
    smtc_.IsPreviousEnabled(availability_.can_previous);
    smtc_.IsNextEnabled(availability_.can_next);

    switch (info_.playback_state) {
        case PODAURA_PLAYBACK_STATE_PLAYING:
            smtc_.PlaybackStatus(MediaPlaybackStatus::Playing);
            break;
        case PODAURA_PLAYBACK_STATE_PAUSED:
            smtc_.PlaybackStatus(MediaPlaybackStatus::Paused);
            break;
        default:
            smtc_.PlaybackStatus(MediaPlaybackStatus::Stopped);
            break;
    }

    auto updater = smtc_.DisplayUpdater();
    if (info_.media_type == PODAURA_MEDIA_TYPE_VIDEO) {
        updater.Type(MediaPlaybackType::Video);
        auto properties = updater.VideoProperties();
        properties.Title(info_.title);
        properties.Subtitle(info_.artist);
    } else {
        updater.Type(MediaPlaybackType::Music);
        auto properties = updater.MusicProperties();
        properties.Title(info_.title);
        properties.Artist(info_.artist);
        properties.AlbumTitle(info_.album);
    }
    if (artwork_id_ != info_.artwork_id) {
        artwork_id_ = info_.artwork_id;
        artwork_reference_ = create_artwork_reference();
    }
    updater.Thumbnail(artwork_reference_);
    updater.Update();

    SystemMediaTransportControlsTimelineProperties timeline;
    if (info_.has_duration) {
        const double duration = info_.duration_seconds;
        const double position = info_.has_elapsed_time
                                ? std::clamp(info_.elapsed_seconds, 0.0, duration)
                                : 0.0;
        timeline.StartTime(seconds_to_timespan(0.0));
        timeline.MinSeekTime(seconds_to_timespan(
                availability_.can_seek ? 0.0 : position
        ));
        timeline.Position(seconds_to_timespan(position));
        timeline.MaxSeekTime(seconds_to_timespan(
                availability_.can_seek ? duration : position
        ));
        timeline.EndTime(seconds_to_timespan(duration));
    } else {
        const auto zero = seconds_to_timespan(0.0);
        timeline.StartTime(zero);
        timeline.MinSeekTime(zero);
        timeline.Position(zero);
        timeline.MaxSeekTime(zero);
        timeline.EndTime(zero);
    }
    smtc_.UpdateTimelineProperties(timeline);
}

void Session::clear_smtc_locked() {
    if (!smtc_) return;
    smtc_.IsPlayEnabled(false);
    smtc_.IsPauseEnabled(false);
    smtc_.IsPreviousEnabled(false);
    smtc_.IsNextEnabled(false);
    smtc_.PlaybackStatus(MediaPlaybackStatus::Closed);
    auto updater = smtc_.DisplayUpdater();
    updater.ClearAll();
    updater.Update();
    smtc_.IsEnabled(false);
}

} // namespace podaura::windows_media
