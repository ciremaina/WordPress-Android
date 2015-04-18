package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.DividerItemDecoration;

import java.util.Map;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends ActionBarActivity
        implements SitePickerAdapter.OnSiteClickListener,
                   SitePickerAdapter.OnMultiSelectListener {

    public static final String KEY_LOCAL_ID = "local_id";
    private static final String KEY_BLOG_ID = "blog_id";

    private RecyclerView mRecycler;
    private static int mBlavatarSz;

    private SitePickerAdapter mAdapter;
    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mRecycler = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        Button btnAddSite = (Button) findViewById(R.id.btn_add_site);
        btnAddSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.addSelfHostedSiteForResult(SitePickerActivity.this);
            }
        });

        mRecycler.setAdapter(getAdapter());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_left);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SignInActivity.CREATE_ACCOUNT_REQUEST:
                if (resultCode != RESULT_CANCELED) {
                    getAdapter().loadSites();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (!isFinishing()) {
            getAdapter().loadSites();
        }
    }

    private boolean hasAdapter() {
        return mAdapter != null;
    }

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new SitePickerAdapter(this);
            mAdapter.setOnSiteClickListener(this);
            mAdapter.setOnMultiSelectedListener(this);
        }
        return mAdapter;
    }

    @Override
    public void onMultiSelectEnabled() {
        if (mActionMode == null) {
            startSupportActionMode(new ActionModeCallback());
            updateActionModeTitle();
        }
    }

    @Override
    public void onSelectedCountChanged(int numSelected) {
        if (mActionMode != null) {
            if (numSelected == 0) {
                mActionMode.finish();
            } else {
                updateActionModeTitle();
                mActionMode.invalidate();
            }
        }
    }

    @Override
    public void onSiteClick(SiteRecord site) {
        if (mActionMode == null) {
            Intent data = new Intent();
            data.putExtra(KEY_LOCAL_ID, site.localId);
            data.putExtra(KEY_BLOG_ID, site.blogId);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private void updateActionModeTitle() {
        if (mActionMode != null && hasAdapter()) {
            int numSelected = getAdapter().getSelectionCount();
            if (numSelected > 0) {
                mActionMode.setTitle(Integer.toString(numSelected));
            } else {
                mActionMode.setTitle("");
            }
        }
    }

    /**
     * SiteRecord is a simplified version of the full account (blog) record
     */
    static class SiteRecord {
        final int localId;
        final String blogId;
        final String blogName;
        final String hostName;
        final String url;
        final String blavatarUrl;
        final boolean isHidden;

        SiteRecord(Map<String, Object> account) {
            localId = MapUtils.getMapInt(account, "id");
            blogId = MapUtils.getMapStr(account, "blogId");
            blogName = BlogUtils.getBlogNameFromAccountMap(account);
            hostName = BlogUtils.getHostNameFromAccountMap(account);
            url = MapUtils.getMapStr(account, "url");
            blavatarUrl = GravatarUtils.blavatarFromUrl(url, mBlavatarSz);
            isHidden = MapUtils.getMapBool(account, "isHidden");
        }

        String getBlogNameOrHostName() {
            if (TextUtils.isEmpty(blogName)) {
                return hostName;
            }
            return blogName;
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int numSelected = getAdapter().getSelectionCount();
            if (numSelected == 0) {
                return false;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            getAdapter().setEnableMultiSelect(false);
            mActionMode = null;
        }
    }
}
