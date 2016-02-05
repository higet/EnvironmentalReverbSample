package com.example.atsample;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.audiofx.AudioEffect;

public class PlaybackRunnable implements Runnable {

	public static final int SAMPLE_RATE = 44100;
	public static final int CHANNEL = 2;

	private boolean mDisposeRequested;
	private AudioTrack mAudioTrack;
	private int mFreq;
	private AudioEffect mReverb;

	public PlaybackRunnable(AudioTrack audioTrack, AudioEffect reverb) {
		mAudioTrack = audioTrack;
		mReverb =reverb;
	}

	@Override
	public void run() {
		final int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
				CHANNEL, AudioFormat.ENCODING_PCM_16BIT);
		int bufsize = minBufferSize * 5;
		short buf[] = new short[bufsize];
		int k = 0;
		long i = 0;
		while (true) {
			if (mDisposeRequested) {
				mAudioTrack.release();
				mReverb.release();
				return;
			}
			// 再生する波形の生成
			if (k++ % 2 == 0) {
				for (int j = 0; j < bufsize; j++) {
					buf[(int) (i % bufsize)] = (short) (Math.sin(2 * Math.PI
							* mFreq * ((i % SAMPLE_RATE) * 1.0f / SAMPLE_RATE)) * Short.MAX_VALUE);
					i++;
				}
			} else {
				for (int j = 0; j < bufsize; j++) {
					buf[j] = 0;
				}
			}
			int totalWrttenSize = 0;
			while (true) {
				if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
					return;
				}
				int writtenSize = mAudioTrack.write(buf, totalWrttenSize,
						bufsize - totalWrttenSize);
				totalWrttenSize += writtenSize;
				if (writtenSize == 0) {
					break;
				}
			}
		}
	}

	public void setFreq(int freq) {
		mFreq = freq;
	}

	public void dispose() {
		mDisposeRequested = true;
	}

}
