/* SPDX-License-Identifier: LGPL-2.1-or-later */
#pragma once
#include <libplacebo/swapchain.h>
struct ra_ctx;
bool podaura_begin_frame(struct ra_ctx *ctx);
void podaura_discard_frame(struct ra_ctx *ctx);
bool podaura_start_frame(struct ra_ctx *ctx, struct pl_swapchain_frame *frame);
bool podaura_submit_frame(struct ra_ctx *ctx);
