package com.example.atsample;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AudioEffect.Descriptor;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.PresetReverb;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	private AudioTrack mAudioTrack;
	protected int DEFAULT_FREQ = 440;
	private AudioEffect mReverb;

	private PlaybackRunnable mLastPlaybackRunnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final Descriptor[] effects = AudioEffect.queryEffects();
		for (final Descriptor effect : effects) {
			// EnvironmentalReverbの記述はある。
			//QTI Insert Environmental Reverb, type: c2e5d5f0-94bd-4763-9cac-4e234d06839e
			//QTI Auxiliary Environmental Reverb, type: c2e5d5f0-94bd-4763-9cac-4e234d06839e
			Log.d("ReverbSample",
					effect.name.toString() + ", type: "
							+ effect.type.toString());
		}

		// 再生する音の周波数を変えるためのシークバー
		SeekBar sb = (SeekBar) findViewById(R.id.seekbarFreq);
		sb.setMax(4000);
		sb.setProgress(DEFAULT_FREQ);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser && mLastPlaybackRunnable != null) {
					mLastPlaybackRunnable.setFreq(progress);
				}
			}
		});

		// ReverbのON/OFF切替
		CheckBox cb = (CheckBox) findViewById(R.id.cbEnableReverb);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mReverb.setEnabled(isChecked);
				if (isChecked) {
					// これを外すと雑音はなくなる。が、依然としてリバーブは効かない
					mAudioTrack.attachAuxEffect(mReverb.getId());
					mAudioTrack.setAuxEffectSendLevel(1.0f);
				} else {
					mAudioTrack.attachAuxEffect(-1);
				}
			}
		});
	}

	void createAudioTrack(boolean useEnvironmentalReverb,
			boolean fixAudioSessionIdToZero) {

		if (mLastPlaybackRunnable != null) {
			// 前の再生ループが動いていたら止める（古いAudioTrackもreleaseされる)
			mLastPlaybackRunnable.dispose();
		}
		final int minBufferSize = AudioTrack.getMinBufferSize(
				PlaybackRunnable.SAMPLE_RATE, PlaybackRunnable.CHANNEL,
				AudioFormat.ENCODING_PCM_16BIT);

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				PlaybackRunnable.SAMPLE_RATE, PlaybackRunnable.CHANNEL,
				AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
				AudioTrack.MODE_STREAM);

		if (useEnvironmentalReverb) {
			EnvironmentalReverb environmentalReverb;
			if (fixAudioSessionIdToZero) {
				// 以下は（少なくとも）SHV31で大音量の雑音（音量設定無視）になる
				// （少なくとも）Nexus5, Nexus5Xではこれでも正常にリバーブが効く
				environmentalReverb = new EnvironmentalReverb(0, 0);
			} else {
				// 以下は（少なくとも）SHV31で効かない、
				// （少なくとも）Nexus5, Nexus5Xではこれでリバーブが効く
				environmentalReverb = new EnvironmentalReverb(0,
						mAudioTrack.getAudioSessionId());
			}

			// ConcertHall相当のリバーブ設定
			environmentalReverb.setRoomLevel((short) -1000);
			environmentalReverb.setRoomHFLevel((short) -500);
			environmentalReverb.setDecayTime(3920);
			environmentalReverb.setDecayHFRatio((short) 700);
			environmentalReverb.setReflectionsLevel((short) -1230);
			environmentalReverb.setReflectionsDelay(20);
			environmentalReverb.setReverbLevel((short) -2);
			environmentalReverb.setReverbDelay(29);
			environmentalReverb.setDiffusion((short) 1000);
			environmentalReverb.setDensity((short) 1000);
			mReverb = environmentalReverb;
		} else {
			// こちらはSHV31でもきく
			PresetReverb presetReverb = new PresetReverb(0,
					mAudioTrack.getAudioSessionId());
			presetReverb.setPreset(PresetReverb.PRESET_LARGEHALL);
			mAudioTrack.setAuxEffectSendLevel(1.0f);
			mReverb = presetReverb;
		}

		CheckBox cb = (CheckBox) findViewById(R.id.cbEnableReverb);
		cb.setChecked(false);

		mLastPlaybackRunnable = new PlaybackRunnable(mAudioTrack, mReverb);
		mLastPlaybackRunnable
				.setFreq(((SeekBar) findViewById(R.id.seekbarFreq))
						.getProgress());
		new Thread(mLastPlaybackRunnable).start();
	}

	public void onClickCreateAudioTrack_PresetReverb(View view) {
		writeMessage("PresetReverbを用意しました");
		createAudioTrack(false, false);
	}

	public void onClickCreateAudioTrack_EnviromentalReverb_Id0(View view) {
		writeMessage("audioSessionに0を指定したEnvironmentalReverbを用意しました");
		createAudioTrack(true, true);
	}

	public void onClickCreateAudioTrack_EnviromentalReverb_Id_equals_AudioTrack(
			View view) {
		writeMessage("AudioTrackのaudioSessionIdを指定したEnvironmentalReverbを用意しました");
		createAudioTrack(true, false);
	}

	public void onStartButtonClick(View view) {
		if (mAudioTrack == null) {
			writeMessage("AudioTrackを先に作ってください");
		} else {
			mAudioTrack.play();
		}
	}

	private void writeMessage(String message) {
		TextView tv = (TextView) findViewById(R.id.tvStatus);
		tv.setText(message);
	}

}
