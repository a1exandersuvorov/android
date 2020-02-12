/*
 *  Copyright 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodec;

/** Container class for static constants and helpers used with MediaCodec. */
class MediaCodecUtils {
  private static final String TAG = "MediaCodecUtils";

  // Prefixes for supported hardware encoder/decoder component names.
  static final String EXYNOS_PREFIX = "OMX.Exynos.";
  static final String INTEL_PREFIX = "OMX.Intel.";
  static final String NVIDIA_PREFIX = "OMX.Nvidia.";
  static final String QCOM_PREFIX = "OMX.qcom.";

  // NV12 color format supported by QCOM codec, but not declared in MediaCodec -
  // see /hardware/qcom/media/mm-core/inc/OMX_QCOMExtns.h
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar32m4ka = 0x7FA30C01;
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar16m4ka = 0x7FA30C02;
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar64x32Tile2m8ka = 0x7FA30C03;
  static final int COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m = 0x7FA30C04;

  // Color formats supported by hardware decoder - in order of preference.
  static final int[] DECODER_COLOR_FORMATS = new int[] {CodecCapabilities.COLOR_FormatYUV420Planar,
      CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
      CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
      MediaCodecUtils.COLOR_QCOM_FORMATYVU420PackedSemiPlanar32m4ka,
      MediaCodecUtils.COLOR_QCOM_FORMATYVU420PackedSemiPlanar16m4ka,
      MediaCodecUtils.COLOR_QCOM_FORMATYVU420PackedSemiPlanar64x32Tile2m8ka,
      MediaCodecUtils.COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m};

  // Color formats supported by hardware encoder - in order of preference.
  static final int[] ENCODER_COLOR_FORMATS = {
      MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
      MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
      MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
      MediaCodecUtils.COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m};

  static Integer selectColorFormat(int[] supportedColorFormats, CodecCapabilities capabilities) {
    for (int supportedColorFormat : supportedColorFormats) {
      for (int codecColorFormat : capabilities.colorFormats) {
        if (codecColorFormat == supportedColorFormat) {
          return codecColorFormat;
        }
      }
    }
    return null;
  }

  static boolean codecSupportsType(MediaCodecInfo info, VideoCodecType type) {
    for (String mimeType : info.getSupportedTypes()) {
      if (type.mimeType().equals(mimeType)) {
        return true;
      }
    }
    return false;
  }

  private MediaCodecUtils() {
    // This class should not be instantiated.
  }
}