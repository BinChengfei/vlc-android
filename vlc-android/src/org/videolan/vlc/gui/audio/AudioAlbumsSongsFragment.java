/*****************************************************************************
 * AudioListActivity.java
 *****************************************************************************
 * Copyright © 2011-2012 VLC authors and VideoLAN
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.widget.SlidingTabLayout;

import org.videolan.libvlc.LibVlcUtil;
import org.videolan.vlc.MediaLibrary;
import org.videolan.vlc.MediaWrapper;
import org.videolan.vlc.R;
import org.videolan.vlc.audio.AudioServiceController;
import org.videolan.vlc.gui.CommonDialogs;
import org.videolan.vlc.gui.SecondaryActivity;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.Util;
import org.videolan.vlc.util.VLCRunnable;
import org.videolan.vlc.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AudioAlbumsSongsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    public final static String TAG = "VLC/AudioAlbumsSongsFragment";

    AudioServiceController mAudioController;
    private MediaLibrary mMediaLibrary;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;
    private AudioBrowserListAdapter mSongsAdapter;
    private AudioBrowserListAdapter mAlbumsAdapter;

    public final static String EXTRA_NAME = "name";
    public final static String EXTRA_NAME2 = "name2";
    public final static String EXTRA_MODE = "mode";

    public final static int MODE_ALBUM = 0;
    public final static int MODE_SONG = 1;
    public final static int MODE_TOTAL = 2; // Number of audio browser modes

    private ArrayList<MediaWrapper> mMediaList;
    private String mTitle;

    /* All subclasses of Fragment must include a public empty constructor. */
    public AudioAlbumsSongsFragment() { }

    public void setMediaList(ArrayList<MediaWrapper> mediaList, String title) {
        mMediaList = mediaList;
        mTitle = title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbumsAdapter = new AudioBrowserListAdapter(getActivity(), AudioBrowserListAdapter.ITEM_WITH_COVER);
        mSongsAdapter = new AudioBrowserListAdapter(getActivity(), AudioBrowserListAdapter.ITEM_WITH_COVER);

        mAlbumsAdapter.setContextPopupMenuListener(mContextPopupMenuListener);
        mSongsAdapter.setContextPopupMenuListener(mContextPopupMenuListener);

        mAudioController = AudioServiceController.getInstance();
        mMediaLibrary = MediaLibrary.getInstance();
        if (savedInstanceState != null)
            setMediaList(savedInstanceState.<MediaWrapper>getParcelableArrayList("list"), savedInstanceState.getString("title"));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.audio_albums_songs, container, false);

        ListView albumsList = (ListView) v.findViewById(R.id.albums);
        ListView songsList = (ListView) v.findViewById(R.id.songs);

        List<View> lists = Arrays.asList((View)albumsList, songsList);
        String[] titles = new String[] {getString(R.string.albums), getString(R.string.songs)};
        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(MODE_TOTAL-1);
        mViewPager.setAdapter(new AudioPagerAdapter(lists, titles));

        mViewPager.setOnTouchListener(mSwipeFilter);
        mSlidingTabLayout = (SlidingTabLayout) v.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_layout, R.id.tab_title);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);

        songsList.setAdapter(mSongsAdapter);
        albumsList.setAdapter(mAlbumsAdapter);

        songsList.setOnItemClickListener(songsListener);
        albumsList.setOnItemClickListener(albumsListener);

        registerForContextMenu(albumsList);
        registerForContextMenu(songsList);

        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeLayout);

        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange700);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        songsList.setOnScrollListener(mScrollListener);
        albumsList.setOnScrollListener(mScrollListener);

        getActivity().setTitle(mTitle);
        return v;
    }

    AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener(){
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {}
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            mSwipeRefreshLayout.setEnabled(firstVisibleItem == 0);
        }
    };

    @Override
    public void onRefresh() {
        updateList();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("list", mMediaList);
        outState.putString("title", mTitle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.audio_list_browser, menu);
        int position = 0;
        if (menuInfo instanceof AdapterContextMenuInfo)
            position = ((AdapterContextMenuInfo)menuInfo).position;
        setContextMenuItems(menu, v, position);
    }

    private void setContextMenuItems(Menu menu, View v, int position) {
        if (mViewPager.getCurrentItem() != MODE_SONG || mSongsAdapter.getItem(position).mIsSeparator) {
            menu.setGroupVisible(R.id.songs_view_only, false);
            menu.setGroupVisible(R.id.phone_only, false);
        }
        if (!AndroidDevices.isPhone())
            menu.setGroupVisible(R.id.phone_only, false);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menu) {
        if(!getUserVisibleHint())
            return super.onContextItemSelected(menu);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu.getMenuInfo();
        if (info != null && handleContextItemSelected(menu, info.position))
            return true;
        return super.onContextItemSelected(menu);
    }

    private boolean handleContextItemSelected(MenuItem item, int position) {
        ContextMenuInfo menuInfo = item.getMenuInfo();
        if (menuInfo == null)
            return false;

        int startPosition;
        int groupPosition;
        List<MediaWrapper> medias;
        int id = item.getItemId();

        boolean useAllItems = id == R.id.audio_list_browser_play_all;
        boolean append = id == R.id.audio_list_browser_append;

        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
            groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        }
        else
            groupPosition = position;

        if (id == R.id.audio_list_browser_delete) {
            AlertDialog alertDialog = CommonDialogs.deleteMedia(
                    getActivity(),
                    mSongsAdapter.getMedias(groupPosition).get(0).getLocation(),
                    new VLCRunnable(mSongsAdapter.getItem(groupPosition)) {
                        @Override
                        public void run(Object o) {
                            AudioBrowserListAdapter.ListItem listItem = (AudioBrowserListAdapter.ListItem)o;
                            MediaWrapper media = listItem.mMediaList.get(0);
                            mMediaLibrary.getMediaItems().remove(media);
                            mSongsAdapter.removeMedia(media);
                            mAlbumsAdapter.removeMedia(media);
                            mAudioController.removeLocation(media.getLocation());
                        }
                    });
            alertDialog.show();
            return true;
        }

        if (id == R.id.audio_list_browser_set_song) {
            AudioUtil.setRingtone(mSongsAdapter.getItem(groupPosition).mMediaList.get(0), getActivity());
            return true;
        }

        if (useAllItems) {
            medias = new ArrayList<MediaWrapper>();
            startPosition = mSongsAdapter.getListWithPosition(medias, groupPosition);
        }
        else {
            startPosition = 0;
            switch (mViewPager.getCurrentItem())
            {
                case MODE_ALBUM: // albums
                    medias = mAlbumsAdapter.getMedias(groupPosition);
                    break;
                case MODE_SONG: // songs
                    medias = mSongsAdapter.getMedias(groupPosition);
                    break;
                default:
                    return true;
            }
        }

        if (append)
            mAudioController.append(medias);
        else
            mAudioController.load(medias, startPosition);

        return super.onContextItemSelected(item);
    }

    private void updateList() {
        if (mMediaList == null || getActivity() == null)
            return;

        final Activity activity = getActivity();

        mAlbumsAdapter.clear();
        mSongsAdapter.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(mMediaList, MediaComparators.byAlbum);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mMediaList.size(); ++i) {
                            MediaWrapper media = mMediaList.get(i);
                            mAlbumsAdapter.addSeparator(Util.getMediaReferenceArtist(activity, media), media);
                            mAlbumsAdapter.add(Util.getMediaAlbum(activity, media), null, media);
                            mSongsAdapter.addSeparator(Util.getMediaAlbum(activity, media), media);
                        }
                        mSongsAdapter.sortByAlbum();
                        mAlbumsAdapter.notifyDataSetChanged();
                        mSongsAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    OnItemClickListener albumsListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int p, long id) {
            ArrayList<MediaWrapper> mediaList = mAlbumsAdapter.getMedias(p);
            Intent i = new Intent(getActivity(), SecondaryActivity.class);
            i.putExtra("fragment", SecondaryActivity.ALBUM);
            i.putParcelableArrayListExtra("list", mediaList);
            i.putExtra("filter", mAlbumsAdapter.getTitle(p));
            startActivity(i);
        }
    };

    OnItemClickListener songsListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int p, long id) {
            List<MediaWrapper> media = mSongsAdapter.getItem(p).mMediaList;
            mAudioController.load(media, 0);
        }
    };

    AudioBrowserListAdapter.ContextPopupMenuListener mContextPopupMenuListener
            = new AudioBrowserListAdapter.ContextPopupMenuListener() {

        @Override
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void onPopupMenu(View anchor, final int position) {
            if (!LibVlcUtil.isHoneycombOrLater()) {
                // Call the "classic" context menu
                anchor.performLongClick();
                return;
            }

            PopupMenu popupMenu = new PopupMenu(getActivity(), anchor);
            popupMenu.getMenuInflater().inflate(R.menu.audio_list_browser, popupMenu.getMenu());
            setContextMenuItems(popupMenu.getMenu(), anchor, position);

            popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return handleContextItemSelected(item, position);
                }
            });
            popupMenu.show();
        }

    };

    /*
     * Disable Swipe Refresh while scrolling horizontally
     */
    private View.OnTouchListener mSwipeFilter = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mSwipeRefreshLayout.setEnabled(false);
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    mSwipeRefreshLayout.setEnabled(true);
                    break;
            }
            return false;
        }
    };

    public void clear(){
        mAlbumsAdapter.clear();
        mSongsAdapter.clear();
    }
}
