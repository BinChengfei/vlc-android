/*****************************************************************************
 * AudioPlayer.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.gui.audio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.videolan.vlc.MediaWrapper;
import org.videolan.vlc.PlaybackService;
import org.videolan.vlc.PlaybackServiceClient;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.AudioPlayerContainerActivity;
import org.videolan.vlc.gui.PreferencesActivity;
import org.videolan.vlc.gui.audio.widget.CoverMediaSwitcher;
import org.videolan.vlc.gui.audio.widget.HeaderMediaSwitcher;
import org.videolan.vlc.gui.dialogs.AdvOptionsDialog;
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.Util;
import org.videolan.vlc.widget.AudioMediaSwitcher.AudioMediaSwitcherListener;

import java.util.ArrayList;
import java.util.List;

public class AudioPlayer extends Fragment implements PlaybackServiceClient.Callback, View.OnClickListener {
    public static final String TAG = "VLC/AudioPlayer";

    private ProgressBar mProgressBar;
    private HeaderMediaSwitcher mHeaderMediaSwitcher;
    private CoverMediaSwitcher mCoverMediaSwitcher;
    private TextView mTime;
    private TextView mHeaderTime;
    private TextView mLength;
    private ImageButton mPlayPause;
    private ImageButton mHeaderPlayPause;
    private ImageButton mNext;
    private ImageButton mPrevious;
    private ImageButton mShuffle;
    private ImageButton mRepeat;
    private ImageButton mAdvFunc;
    private ImageButton mPlaylistSwitch, mPlaylistSave;
    private SeekBar mTimeline;
    private AudioPlaylistView mSongsList;

    ViewSwitcher mSwitcher;

    private PlaybackServiceClient mClient;
    private boolean mShowRemainingTime = false;
    private boolean mPreviewingSeek = false;

    private AudioPlaylistAdapter mSongsListAdapter;

    private boolean mAdvFuncVisible;
    private boolean mPlaylistSwitchVisible;
    private boolean mPlaylistSaveVisible;
    private boolean mHeaderPlayPauseVisible;
    private boolean mProgressBarVisible;
    private boolean mHeaderTimeVisible;

    // Tips
    private static final String PREF_PLAYLIST_TIPS_SHOWN = "playlist_tips_shown";
    private static final String PREF_AUDIOPLAYER_TIPS_SHOWN = "audioplayer_tips_shown";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSongsListAdapter = new AudioPlaylistAdapter(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        mClient = AudioPlayerContainerActivity.getPlaybackClient(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audio_player, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        mHeaderMediaSwitcher = (HeaderMediaSwitcher) v.findViewById(R.id.audio_media_switcher);
        mHeaderMediaSwitcher.setAudioMediaSwitcherListener(mHeaderMediaSwitcherListener);
        mCoverMediaSwitcher = (CoverMediaSwitcher) v.findViewById(R.id.cover_media_switcher);
        mCoverMediaSwitcher.setAudioMediaSwitcherListener(mCoverMediaSwitcherListener);

        mTime = (TextView) v.findViewById(R.id.time);
        mHeaderTime = (TextView) v.findViewById(R.id.header_time);
        mLength = (TextView) v.findViewById(R.id.length);
        mPlayPause = (ImageButton) v.findViewById(R.id.play_pause);
        mHeaderPlayPause = (ImageButton) v.findViewById(R.id.header_play_pause);
        mNext = (ImageButton) v.findViewById(R.id.next);
        mPrevious = (ImageButton) v.findViewById(R.id.previous);
        mShuffle = (ImageButton) v.findViewById(R.id.shuffle);
        mRepeat = (ImageButton) v.findViewById(R.id.repeat);
        mAdvFunc = (ImageButton) v.findViewById(R.id.adv_function);
        mPlaylistSwitch = (ImageButton) v.findViewById(R.id.playlist_switch);
        mPlaylistSave = (ImageButton) v.findViewById(R.id.playlist_save);
        mTimeline = (SeekBar) v.findViewById(R.id.timeline);

        mSongsList = (AudioPlaylistView) v.findViewById(R.id.songs_list);
        mSongsList.setAdapter(mSongsListAdapter);

        mSwitcher = (ViewSwitcher) v.findViewById(R.id.view_switcher);
        mSwitcher.setInAnimation(getActivity(), android.R.anim.fade_in);
        mSwitcher.setOutAnimation(getActivity(), android.R.anim.fade_out);

        mAdvFuncVisible = false;
        mPlaylistSwitchVisible = false;
        mPlaylistSaveVisible = false;
        mHeaderPlayPauseVisible = true;
        mProgressBarVisible = true;
        mHeaderTimeVisible = true;
        restoreHedaderButtonVisibilities();

        mTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimeLabelClick(v);
            }
        });
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPauseClick(v);
            }
        });
        mPlayPause.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onStopClick(v);
                return true;
            }
        });
        mHeaderPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPauseClick(v);
            }
        });
        mHeaderPlayPause.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onStopClick(v);
                return true;
            }
        });
        mNext.setOnTouchListener(new LongSeekListener(true,
                Util.getResourceFromAttribute(getActivity(), R.attr.ic_next),
                R.drawable.ic_next_pressed));
        mPrevious.setOnTouchListener(new LongSeekListener(false,
                Util.getResourceFromAttribute(getActivity(), R.attr.ic_previous),
                R.drawable.ic_previous_pressed));
        mShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShuffleClick(v);
            }
        });
        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRepeatClick(v);
            }
        });
        mAdvFunc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdvancedOptions(v);
            }
        });
        mPlaylistSave.setOnClickListener(this);
        mPlaylistSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitcher.showNext();
                if (mSwitcher.getDisplayedChild() == 0)
                    mPlaylistSwitch.setImageResource(Util.getResourceFromAttribute(getActivity(),
                                                     R.attr.ic_playlist_on));
                else
                    mPlaylistSwitch.setImageResource(Util.getResourceFromAttribute(getActivity(),
                                                     R.attr.ic_playlist));
            }
        });
        mSongsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int p, long id) {
                if (mClient.isConnected())
                    mClient.playIndex(p);
            }
        });
        mSongsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                getActivity().openContextMenu(view);
                return true;
            }
        });
        mSongsList.setOnItemDraggedListener(new AudioPlaylistView.OnItemDraggedListener() {
            @Override
            public void onItemDragged(int positionStart, int positionEnd) {
                if (mClient.isConnected())
                    mClient.moveItem(positionStart, positionEnd);
            }
        });
        mSongsList.setOnItemRemovedListener(new AudioPlaylistView.OnItemRemovedListener() {
            @Override
            public void onItemRemoved(int position) {
                if (mClient.isConnected())
                    mClient.remove(position);
                update();
            }
        });
        registerForContextMenu(mSongsList);

        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getView().cancelLongPress();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.audio_player, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint() && item.getMenuInfo() instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            if(info == null) // info can be null
                return super.onContextItemSelected(item);
            int id = item.getItemId();

            if(id == R.id.audio_player_mini_remove) {
                Log.d(TAG, "Context menu removing " + info.position);
                if (mClient.isConnected())
                    mClient.remove(info.position);
                return true;
            }
            return super.onContextItemSelected(item);
        } else
            return false;
    }

    /**
     * Show the audio player from an intent
     *
     * @param context The context of the activity
     */
    public static void start(Context context) {
        Intent intent = new Intent();
        intent.setAction(AudioPlayerContainerActivity.ACTION_SHOW_PLAYER);
        context.getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onConnected() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void update() {
        if (!mClient.isConnected())
            return;

        if (mClient.hasMedia()) {
            SharedPreferences mSettings= PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (mSettings.getBoolean(PreferencesActivity.VIDEO_RESTORE, false)){
                Util.commitPreferences(mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, false));
                mClient.handleVout();
                return;
            }
            show();
        } else {
            hide();
            return;
        }

        mHeaderMediaSwitcher.updateMedia(mClient);
        mCoverMediaSwitcher.updateMedia(mClient);

        FragmentActivity act = getActivity();

        if (mClient.isPlaying()) {
            mPlayPause.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_pause));
            mPlayPause.setContentDescription(getString(R.string.pause));
            mHeaderPlayPause.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_pause));
            mHeaderPlayPause.setContentDescription(getString(R.string.pause));
        } else {
            mPlayPause.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_play));
            mPlayPause.setContentDescription(getString(R.string.play));
            mHeaderPlayPause.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_play));
            mHeaderPlayPause.setContentDescription(getString(R.string.play));
        }
        if (mClient.isShuffling()) {
            mShuffle.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_shuffle_on));
            mShuffle.setContentDescription(getResources().getString(R.string.shuffle_on));
        } else {
            mShuffle.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_shuffle));
            mShuffle.setContentDescription(getResources().getString(R.string.shuffle));
        }
        switch(mClient.getRepeatType()) {
        case None:
            mRepeat.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_repeat));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat));
            break;
        case Once:
            mRepeat.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_repeat_one));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat_single));
            break;
        default:
        case All:
            mRepeat.setImageResource(Util.getResourceFromAttribute(act, R.attr.ic_repeat_on));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat_all));
            break;
        }

        final List<String> mediaLocations = mClient.getMediaLocations();
        mShuffle.setVisibility(mediaLocations != null && mediaLocations.size() > 2 ? View.VISIBLE : View.INVISIBLE);
        if (mClient.hasNext())
            mNext.setVisibility(ImageButton.VISIBLE);
        else
            mNext.setVisibility(ImageButton.INVISIBLE);
        if (mClient.hasPrevious())
            mPrevious.setVisibility(ImageButton.VISIBLE);
        else
            mPrevious.setVisibility(ImageButton.INVISIBLE);
        mTimeline.setOnSeekBarChangeListener(mTimelineListner);

        updateList();
    }

    @Override
    public void updateProgress() {
        if (!mClient.isConnected())
            return;
        int time = mClient.getTime();
        int length = mClient.getLength();

        mHeaderTime.setText(Strings.millisToString(time));
        mLength.setText(Strings.millisToString(length));
        mTimeline.setMax(length);
        mProgressBar.setMax(length);

        if(!mPreviewingSeek) {
            mTime.setText(Strings.millisToString(mShowRemainingTime ? time-length : time));
            mTimeline.setProgress(time);
            mProgressBar.setProgress(time);
        }
    }

    @Override
    public void onMediaPlayedAdded(MediaWrapper media, int index) {
    }

    @Override
    public void onMediaPlayedRemoved(int index) {
    }

    private void updateList() {
        int currentIndex = -1;
        if (!mClient.isConnected())
            return;

        mSongsListAdapter.clear();

        final List<MediaWrapper> audioList = mClient.getMedias();
        final String currentItem = mClient.getCurrentMediaLocation();

        if (audioList != null) {
            for (int i = 0; i < audioList.size(); i++) {
                final MediaWrapper media = audioList.get(i);
                if (currentItem != null && currentItem.equals(media.getLocation()))
                    currentIndex = i;
                mSongsListAdapter.add(media);
            }
        }
        mSongsListAdapter.setCurrentIndex(currentIndex);
        mSongsList.setSelection(currentIndex);

        mSongsListAdapter.notifyDataSetChanged();
    }

    OnSeekBarChangeListener mTimelineListner = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
            if (fromUser && mClient.isConnected()) {
                mClient.setTime(prog);
                mTime.setText(Strings.millisToString(mShowRemainingTime ? prog- mClient.getLength() : prog));
                mHeaderTime.setText(Strings.millisToString(prog));
        }
    }
    };

    public void onTimeLabelClick(View view) {
        mShowRemainingTime = !mShowRemainingTime;
        update();
    }

    public void onPlayPauseClick(View view) {
        if (!mClient.isConnected())
            return;
        if (mClient.isPlaying()) {
            mClient.pause();
        } else {
            mClient.play();
        }
    }

    public void onStopClick(View view) {
        if (mClient.isConnected())
            mClient.stop();
    }

    public void onNextClick(View view) {
        if (mClient.isConnected())
            mClient.next();
    }

    public void onPreviousClick(View view) {
        if (mClient.isConnected())
            mClient.previous();
    }

    public void onRepeatClick(View view) {
        if (!mClient.isConnected())
            return;

        switch (mClient.getRepeatType()) {
            case None:
                mClient.setRepeatType(PlaybackService.RepeatType.All);
                break;
            case All:
                mClient.setRepeatType(PlaybackService.RepeatType.Once);
                break;
            default:
            case Once:
                mClient.setRepeatType(PlaybackService.RepeatType.None);
                break;
        }
        update();
    }

    public void onShuffleClick(View view) {
        if (mClient.isConnected())
            mClient.shuffle();
        update();
    }

    public void showAdvancedOptions(View v) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        AdvOptionsDialog advOptionsDialog = new AdvOptionsDialog();
        Bundle args = new Bundle();
        args.putInt(AdvOptionsDialog.MODE_KEY, AdvOptionsDialog.MODE_AUDIO);
        advOptionsDialog.setArguments(args);
        advOptionsDialog.show(fm, "fragment_adv_options");
    }

    public void show() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if (activity != null)
            activity.showAudioPlayer();
    }

    public void hide() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if (activity != null)
            activity.hideAudioPlayer();
    }

    /**
     * Set the visibilities of the player header elements.
     * @param advFuncVisible
     * @param playlistSwitchVisible
     * @param headerPlayPauseVisible
     */
    public void setHeaderVisibilities(boolean advFuncVisible, boolean playlistSwitchVisible,
                                      boolean headerPlayPauseVisible, boolean progressBarVisible,
                                      boolean headerTimeVisible, boolean playlistSaveVisible) {
        mAdvFuncVisible = advFuncVisible;
        mPlaylistSwitchVisible = playlistSwitchVisible;
        mHeaderPlayPauseVisible = headerPlayPauseVisible;
        mProgressBarVisible = progressBarVisible;
        mHeaderTimeVisible = headerTimeVisible;
        mPlaylistSaveVisible = playlistSaveVisible;
        restoreHedaderButtonVisibilities();
    }

    private void restoreHedaderButtonVisibilities() {
        mAdvFunc.setVisibility(mAdvFuncVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mPlaylistSwitch.setVisibility(mPlaylistSwitchVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mPlaylistSave.setVisibility(mPlaylistSaveVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mHeaderPlayPause.setVisibility(mHeaderPlayPauseVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mProgressBar.setVisibility(mProgressBarVisible ? ProgressBar.VISIBLE : ProgressBar.GONE);
        mHeaderTime.setVisibility(mHeaderTimeVisible ? TextView.VISIBLE : TextView.GONE);
    }

    private void hideHedaderButtons() {
        mAdvFunc.setVisibility(ImageButton.GONE);
        mPlaylistSwitch.setVisibility(ImageButton.GONE);
        mPlaylistSave.setVisibility(ImageButton.GONE);
        mHeaderPlayPause.setVisibility(ImageButton.GONE);
        mHeaderTime.setVisibility(TextView.GONE);
    }

    private final AudioMediaSwitcherListener mHeaderMediaSwitcherListener = new AudioMediaSwitcherListener() {

        @Override
        public void onMediaSwitching() {}

        @Override
        public void onMediaSwitched(int position) {
            if (!mClient.isConnected())
                return;
            if (position == AudioMediaSwitcherListener.PREVIOUS_MEDIA)
                mClient.previous();
            else if (position == AudioMediaSwitcherListener.NEXT_MEDIA)
                mClient.next();
        }

        @Override
        public void onTouchDown() {
            hideHedaderButtons();
        }

        @Override
        public void onTouchUp() {
            restoreHedaderButtonVisibilities();
        }

        @Override
        public void onTouchClick() {
            AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
            activity.slideUpOrDownAudioPlayer();
        }
    };

    private final AudioMediaSwitcherListener mCoverMediaSwitcherListener = new AudioMediaSwitcherListener() {

        @Override
        public void onMediaSwitching() {}

        @Override
        public void onMediaSwitched(int position) {
            if (!mClient.isConnected())
                return;
            if (position == AudioMediaSwitcherListener.PREVIOUS_MEDIA)
                mClient.previous();
            else if (position == AudioMediaSwitcherListener.NEXT_MEDIA)
                mClient.next();
        }

        @Override
        public void onTouchDown() {}

        @Override
        public void onTouchUp() {}

        @Override
        public void onTouchClick() {}
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playlist_save:
                if (!mClient.isConnected())
                    return;
                FragmentManager fm = getActivity().getSupportFragmentManager();
                SavePlaylistDialog savePlaylistDialog = new SavePlaylistDialog();
                Bundle args = new Bundle();
                args.putParcelableArrayList(SavePlaylistDialog.KEY_TRACKS, (ArrayList<MediaWrapper>) mClient.getMedias());
                savePlaylistDialog.setArguments(args);
                savePlaylistDialog.show(fm, "fragment_save_playlist");
                break;
        }
    }

    class LongSeekListener implements View.OnTouchListener {
        boolean forward;
        int normal, pressed;
        long length;

        public LongSeekListener(boolean forwards, int normalRes, int pressedRes) {
            this.forward = forwards;
            this.normal = normalRes;
            this.pressed = pressedRes;
            this.length = -1;
        }

        int possibleSeek;
        boolean vibrated;
        Runnable seekRunnable = new Runnable() {
            @Override
            public void run() {
                if(!vibrated) {
                    ((android.os.Vibrator) VLCApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE))
                                .vibrate(80);
                    vibrated = true;
                }

                if(forward) {
                    if(length <= 0 || possibleSeek < length)
                        possibleSeek += 4000;
                } else {
                    if(possibleSeek > 4000)
                        possibleSeek -= 4000;
                    else if(possibleSeek <= 4000)
                        possibleSeek = 0;
                }

                mTime.setText(Strings.millisToString(mShowRemainingTime ? possibleSeek-length : possibleSeek));
                mTimeline.setProgress(possibleSeek);
                mProgressBar.setProgress(possibleSeek);
                h.postDelayed(seekRunnable, 50);
            }
        };

        Handler h = new Handler();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!mClient.isConnected())
                return false;
            switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                (forward ? mNext : mPrevious).setImageResource(this.pressed);

                possibleSeek = mClient.getTime();
                mPreviewingSeek = true;
                vibrated = false;
                length = mClient.getLength();

                h.postDelayed(seekRunnable, 1000);
                return true;

            case MotionEvent.ACTION_UP:
                (forward ? mNext : mPrevious).setImageResource(this.normal);
                h.removeCallbacks(seekRunnable);
                mPreviewingSeek = false;

                if(event.getEventTime()-event.getDownTime() < 1000) {
                    if(forward)
                        onNextClick(v);
                    else
                        onPreviousClick(v);
                } else {
                    if(forward) {
                        if(possibleSeek < mClient.getLength())
                            mClient.setTime(possibleSeek);
                        else
                            onNextClick(v);
                    } else {
                        if(possibleSeek > 0)
                            mClient.setTime(possibleSeek);
                        else
                            onPreviousClick(v);
                    }
                }
                return true;
            }
            return false;
        }
    }

    public void showPlaylistTips() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if(activity != null)
            activity.showTipViewIfNeeded(R.layout.audio_playlist_tips, PREF_PLAYLIST_TIPS_SHOWN);
    }

    public void showAudioPlayerTips() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if(activity != null)
            activity.showTipViewIfNeeded(R.layout.audio_player_tips, PREF_AUDIOPLAYER_TIPS_SHOWN);
    }

    /*
     * Override this method to prefent NPE on mFragmentManager reference.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getFragmentManager() != null)
            super.setUserVisibleHint(isVisibleToUser);
    }
}
