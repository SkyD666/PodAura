#include "WindowsMediaSession.h"

#include <commctrl.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

namespace podaura::windows_media {
namespace {

constexpr UINT kPreviousButtonId = 0x5001;
constexpr UINT kPlayPauseButtonId = 0x5002;
constexpr UINT kNextButtonId = 0x5003;

enum class Glyph {
    Previous,
    Play,
    Pause,
    Next,
};

constexpr std::array<const wchar_t *, 2> kWindowsIconFonts{
        L"Segoe Fluent Icons",
        L"Segoe MDL2 Assets",
};

wchar_t system_glyph(Glyph glyph) {
    switch (glyph) {
        case Glyph::Previous:
            return L'\uE892';
        case Glyph::Play:
            return L'\uE768';
        case Glyph::Pause:
            return L'\uE769';
        case Glyph::Next:
            return L'\uE893';
    }
    return L'\0';
}

HFONT create_system_icon_font(HDC device_context, int pixel_size) {
    for (const wchar_t *font_family: kWindowsIconFonts) {
        HFONT font = CreateFontW(
                -pixel_size,
                0,
                0,
                0,
                FW_NORMAL,
                FALSE,
                FALSE,
                FALSE,
                DEFAULT_CHARSET,
                OUT_DEFAULT_PRECIS,
                CLIP_DEFAULT_PRECIS,
                ANTIALIASED_QUALITY,
                DEFAULT_PITCH,
                font_family
        );
        if (font == nullptr) continue;

        HGDIOBJ old_font = SelectObject(device_context, font);
        if (old_font == nullptr || old_font == HGDI_ERROR) {
            DeleteObject(font);
            continue;
        }
        wchar_t selected_family[LF_FACESIZE]{};
        const bool selected_requested_font =
                GetTextFaceW(device_context, LF_FACESIZE, selected_family) > 0 &&
                CompareStringOrdinal(
                        selected_family,
                        -1,
                        font_family,
                        -1,
                        TRUE
                ) == CSTR_EQUAL;
        SelectObject(device_context, old_font);
        if (selected_requested_font) return font;
        DeleteObject(font);
    }
    return nullptr;
}

UINT taskbar_created_message() {
    static const UINT message = RegisterWindowMessageW(L"TaskbarButtonCreated");
    return message;
}

COLORREF taskbar_glyph_color() {
    HIGHCONTRASTW high_contrast{sizeof(HIGHCONTRASTW)};
    if (SystemParametersInfoW(SPI_GETHIGHCONTRAST, sizeof(high_contrast), &high_contrast, 0) &&
        (high_contrast.dwFlags & HCF_HIGHCONTRASTON) != 0) {
        return GetSysColor(COLOR_BTNTEXT);
    }

    DWORD uses_light_theme = 1;
    DWORD size = sizeof(uses_light_theme);
    RegGetValueW(
            HKEY_CURRENT_USER,
            L"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            L"SystemUsesLightTheme",
            RRF_RT_REG_DWORD,
            nullptr,
            &uses_light_theme,
            &size
    );
    return uses_light_theme != 0 ? RGB(24, 24, 24) : RGB(248, 248, 248);
}

void set_pixel(uint32_t *pixels, int size, int x, int y, COLORREF color) {
    if (x < 0 || y < 0 || x >= size || y >= size) return;
    pixels[y * size + x] = 0xFF000000U |
                           (static_cast<uint32_t>(GetRValue(color)) << 16U) |
                           (static_cast<uint32_t>(GetGValue(color)) << 8U) |
                           static_cast<uint32_t>(GetBValue(color));
}

void fill_rect(
        uint32_t *pixels,
        int size,
        int left,
        int top,
        int right,
        int bottom,
        COLORREF color
) {
    for (int y = top; y < bottom; ++y) {
        for (int x = left; x < right; ++x) set_pixel(pixels, size, x, y, color);
    }
}

void fill_triangle(
        uint32_t *pixels,
        int size,
        bool points_right,
        int left,
        int top,
        int right,
        int bottom,
        COLORREF color
) {
    const double middle = (top + bottom - 1) / 2.0;
    const double half_height = std::max(1.0, (bottom - top) / 2.0);
    const int width = std::max(1, right - left);
    for (int x = left; x < right; ++x) {
        const double progress = points_right
                                ? static_cast<double>(right - x) / width
                                : static_cast<double>(x - left + 1) / width;
        const int extent = static_cast<int>(std::round(progress * half_height));
        for (int y = static_cast<int>(middle) - extent;
             y <= static_cast<int>(middle) + extent;
             ++y) {
            set_pixel(pixels, size, x, y, color);
        }
    }
}

bool render_system_glyph(
        HBITMAP bitmap,
        uint32_t *pixels,
        int size,
        Glyph glyph,
        COLORREF color
) {
    HDC device_context = CreateCompatibleDC(nullptr);
    if (device_context == nullptr) return false;

    HGDIOBJ old_bitmap = SelectObject(device_context, bitmap);
    if (old_bitmap == nullptr || old_bitmap == HGDI_ERROR) {
        DeleteDC(device_context);
        return false;
    }

    const int font_size = std::max(12, size * 4 / 5);
    HFONT font = create_system_icon_font(device_context, font_size);
    if (font == nullptr) {
        SelectObject(device_context, old_bitmap);
        DeleteDC(device_context);
        return false;
    }

    HGDIOBJ old_font = SelectObject(device_context, font);
    const int old_background_mode = SetBkMode(device_context, TRANSPARENT);
    const COLORREF old_text_color = SetTextColor(device_context, RGB(255, 255, 255));
    const wchar_t codepoint = system_glyph(glyph);
    RECT bounds{0, 0, size, size};
    const int draw_result = DrawTextW(
            device_context,
            &codepoint,
            1,
            &bounds,
            DT_CENTER | DT_VCENTER | DT_SINGLELINE | DT_NOPREFIX
    );
    GdiFlush();

    SetTextColor(device_context, old_text_color);
    SetBkMode(device_context, old_background_mode);
    SelectObject(device_context, old_font);
    SelectObject(device_context, old_bitmap);
    DeleteObject(font);
    DeleteDC(device_context);
    if (draw_result == 0) return false;

    bool has_visible_pixel = false;
    for (int index = 0; index < size * size; ++index) {
        const uint32_t source = pixels[index];
        const auto coverage = static_cast<uint8_t>(std::max({
                (source >> 16U) & 0xFFU,
                (source >> 8U) & 0xFFU,
                source & 0xFFU,
        }));
        if (coverage == 0) {
            pixels[index] = 0U;
            continue;
        }
        has_visible_pixel = true;
        pixels[index] = (static_cast<uint32_t>(coverage) << 24U) |
                        (static_cast<uint32_t>(GetRValue(color)) << 16U) |
                        (static_cast<uint32_t>(GetGValue(color)) << 8U) |
                        static_cast<uint32_t>(GetBValue(color));
    }
    return has_visible_pixel;
}

void render_fallback_glyph(uint32_t *pixels, int size, Glyph glyph, COLORREF color) {
    const int margin = std::max(3, size / 5);
    const int stroke = std::max(2, size / 7);
    const int gap = std::max(1, size / 16);
    const int triangle_width = std::max(2, (size - margin * 2 - gap) / 2);
    switch (glyph) {
        case Glyph::Previous:
            fill_triangle(
                    pixels, size, false,
                    margin, margin, margin + triangle_width, size - margin, color
            );
            fill_triangle(
                    pixels, size, false,
                    margin + triangle_width + gap, margin,
                    size - margin, size - margin, color
            );
            break;
        case Glyph::Play:
            fill_triangle(
                    pixels, size, true,
                    margin, margin, size - margin, size - margin, color
            );
            break;
        case Glyph::Pause: {
            const int center = size / 2;
            fill_rect(
                    pixels, size,
                    center - stroke - 1, margin, center - 1, size - margin, color
            );
            fill_rect(
                    pixels, size,
                    center + 1, margin, center + stroke + 1, size - margin, color
            );
            break;
        }
        case Glyph::Next:
            fill_triangle(
                    pixels, size, true,
                    margin, margin, margin + triangle_width, size - margin, color
            );
            fill_triangle(
                    pixels, size, true,
                    margin + triangle_width + gap, margin,
                    size - margin, size - margin, color
            );
            break;
    }
}

HICON create_glyph_icon(HWND window, Glyph glyph) {
    const UINT dpi = GetDpiForWindow(window);
    const int icon_size = std::max(16, GetSystemMetricsForDpi(SM_CXICON, dpi));
    BITMAPV5HEADER header{};
    header.bV5Size = sizeof(header);
    header.bV5Width = icon_size;
    header.bV5Height = -icon_size;
    header.bV5Planes = 1;
    header.bV5BitCount = 32;
    header.bV5Compression = BI_BITFIELDS;
    header.bV5RedMask = 0x00FF0000;
    header.bV5GreenMask = 0x0000FF00;
    header.bV5BlueMask = 0x000000FF;
    header.bV5AlphaMask = 0xFF000000;

    void *bits = nullptr;
    HDC screen = GetDC(nullptr);
    HBITMAP color_bitmap = CreateDIBSection(
            screen,
            reinterpret_cast<BITMAPINFO *>(&header),
            DIB_RGB_COLORS,
            &bits,
            nullptr,
            0
    );
    ReleaseDC(nullptr, screen);
    if (color_bitmap == nullptr || bits == nullptr) return nullptr;

    auto *pixels = static_cast<uint32_t *>(bits);
    std::fill(pixels, pixels + icon_size * icon_size, 0U);
    const COLORREF color = taskbar_glyph_color();
    if (!render_system_glyph(color_bitmap, pixels, icon_size, glyph, color)) {
        std::fill(pixels, pixels + icon_size * icon_size, 0U);
        render_fallback_glyph(pixels, icon_size, glyph, color);
    }

    HBITMAP mask_bitmap = CreateBitmap(icon_size, icon_size, 1, 1, nullptr);
    ICONINFO icon_info{};
    icon_info.fIcon = TRUE;
    icon_info.hbmColor = color_bitmap;
    icon_info.hbmMask = mask_bitmap;
    HICON icon = CreateIconIndirect(&icon_info);
    DeleteObject(mask_bitmap);
    DeleteObject(color_bitmap);
    return icon;
}

DWORD button_flags(bool has_media, bool enabled) {
    if (!has_media) return THBF_HIDDEN;
    return enabled ? THBF_ENABLED : THBF_DISABLED;
}

void populate_button(
        THUMBBUTTON &button,
        HWND window,
        UINT id,
        Glyph glyph,
        const std::wstring &tooltip,
        bool has_media,
        bool enabled
) {
    button = {};
    button.dwMask = THB_ICON | THB_TOOLTIP | THB_FLAGS;
    button.iId = id;
    button.hIcon = create_glyph_icon(window, glyph);
    button.dwFlags = static_cast<THUMBBUTTONFLAGS>(button_flags(has_media, enabled));
    wcsncpy_s(button.szTip, tooltip.c_str(), _TRUNCATE);
}

} // namespace

void Session::ensure_taskbar() {
    if (taskbar_) return;
    winrt::com_ptr<ITaskbarList3> taskbar;
    winrt::check_hresult(CoCreateInstance(
            CLSID_TaskbarList,
            nullptr,
            CLSCTX_INPROC_SERVER,
            IID_PPV_ARGS(taskbar.put())
    ));
    winrt::check_hresult(taskbar->HrInit());
    taskbar_ = std::move(taskbar);
}

void Session::update_taskbar(WindowRegistration &registration) {
    if (!taskbar_) return;
    std::array<THUMBBUTTON, 3> buttons{};
    populate_button(
            buttons[0],
            registration.window,
            kPreviousButtonId,
            Glyph::Previous,
            registration.tooltips.previous,
            has_media_,
            availability_.can_previous
    );
    const bool showing_pause = has_media_ && availability_.can_pause;
    populate_button(
            buttons[1],
            registration.window,
            kPlayPauseButtonId,
            showing_pause ? Glyph::Pause : Glyph::Play,
            showing_pause ? registration.tooltips.pause : registration.tooltips.play,
            has_media_,
            availability_.can_toggle
    );
    populate_button(
            buttons[2],
            registration.window,
            kNextButtonId,
            Glyph::Next,
            registration.tooltips.next,
            has_media_,
            availability_.can_next
    );

    HRESULT result;
    if (!registration.toolbar_registered) {
        result = taskbar_->ThumbBarAddButtons(
                registration.window,
                static_cast<UINT>(buttons.size()),
                buttons.data()
        );
        if (FAILED(result)) {
            result = taskbar_->ThumbBarUpdateButtons(
                    registration.window,
                    static_cast<UINT>(buttons.size()),
                    buttons.data()
            );
        }
    } else {
        result = taskbar_->ThumbBarUpdateButtons(
                registration.window,
                static_cast<UINT>(buttons.size()),
                buttons.data()
        );
    }
    registration.toolbar_registered = SUCCEEDED(result);
    for (auto &button: buttons) {
        if (button.hIcon != nullptr) DestroyIcon(button.hIcon);
    }
}

void Session::refresh_taskbars_locked() {
    for (auto &[_, registration]: windows_) {
        if (!registration->refresh_requested) continue;
        try {
            ensure_taskbar();
            update_taskbar(*registration);
        } catch (...) {
            registration->toolbar_registered = false;
        }
        ++registration->refresh_attempts;
        if (registration->toolbar_registered || registration->refresh_attempts >= 100) {
            registration->refresh_requested = false;
        }
    }
}

void Session::hide_taskbar(WindowRegistration &registration) {
    if (!taskbar_ || !registration.toolbar_registered) return;
    const bool old_has_media = has_media_;
    has_media_ = false;
    update_taskbar(registration);
    has_media_ = old_has_media;
}

void Session::on_window_message(
        WindowRegistration &registration,
        UINT message,
        WPARAM w_param
) {
    if (!registration.active.load()) return;
    if (message == taskbar_created_message()) {
        std::scoped_lock lock(mutex_);
        registration.toolbar_registered = false;
        registration.refresh_requested = true;
        registration.refresh_attempts = 0;
        return;
    }
    if (message == WM_THEMECHANGED || message == WM_SETTINGCHANGE || message == WM_DPICHANGED) {
        std::scoped_lock lock(mutex_);
        registration.refresh_requested = true;
        registration.refresh_attempts = 0;
        return;
    }
    if (message == WM_COMMAND && HIWORD(w_param) == THBN_CLICKED) {
        switch (LOWORD(w_param)) {
            case kPreviousButtonId:
                queue_command(PODAURA_MEDIA_COMMAND_PREVIOUS, 0.0);
                break;
            case kPlayPauseButtonId:
                queue_command(PODAURA_MEDIA_COMMAND_TOGGLE_PLAY_PAUSE, 0.0);
                break;
            case kNextButtonId:
                queue_command(PODAURA_MEDIA_COMMAND_NEXT, 0.0);
                break;
            default:
                break;
        }
    }
}

} // namespace podaura::windows_media
