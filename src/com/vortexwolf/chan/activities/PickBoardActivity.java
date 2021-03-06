package com.vortexwolf.chan.activities;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.vortexwolf.chan.R;
import com.vortexwolf.chan.adapters.BoardsListAdapter;
import com.vortexwolf.chan.common.Constants;
import com.vortexwolf.chan.common.Factory;
import com.vortexwolf.chan.common.Websites;
import com.vortexwolf.chan.common.utils.AppearanceUtils;
import com.vortexwolf.chan.common.utils.CompatibilityUtils;
import com.vortexwolf.chan.common.utils.StringUtils;
import com.vortexwolf.chan.db.FavoritesDataSource;
import com.vortexwolf.chan.db.FavoritesEntity;
import com.vortexwolf.chan.interfaces.IUrlBuilder;
import com.vortexwolf.chan.interfaces.IWebsite;
import com.vortexwolf.chan.models.presentation.BoardEntity;
import com.vortexwolf.chan.models.presentation.BoardModel;
import com.vortexwolf.chan.models.presentation.SectionEntity;
import com.vortexwolf.chan.services.NavigationService;
import com.vortexwolf.chan.services.presentation.EditTextDialog;
import com.vortexwolf.chan.settings.ApplicationPreferencesActivity;
import com.vortexwolf.chan.settings.ApplicationSettings;
import com.vortexwolf.chan.settings.SettingsEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class PickBoardActivity extends ListActivity {

    public static final String TAG = "PickBoardActivity";

    private static final Pattern boardCodePattern = Pattern.compile("^\\w+$");

    private FavoritesDataSource mFavoritesDatasource = Factory.resolve(FavoritesDataSource.class);
    private ApplicationSettings mSettings = Factory.resolve(ApplicationSettings.class);
    private NavigationService mNavigationService = Factory.resolve(NavigationService.class);
    private IUrlBuilder mUrlBuilder;

    private IWebsite mWebsite;
    private BoardsListAdapter mAdapter = null;
    private SettingsEntity mCurrentSettings = null;

    private final ArrayList<BoardModel> mBoards = new ArrayList<BoardModel>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mWebsite = Websites.fromName(this.getIntent().getStringExtra(Constants.EXTRA_WEBSITE));
        if (this.mWebsite == null) {
            this.mWebsite = Websites.getDefault();
        }

        this.mUrlBuilder = this.mWebsite.getUrlBuilder();
        this.mCurrentSettings = this.mSettings.getCurrentSettings();

        // TODO: add Default Website to the settings and navigate to it
        if (this.mSettings.getStartPage() != null && Intent.ACTION_MAIN.equals(this.getIntent().getAction())) {
            if (this.navigateBoard(this.mSettings.getStartPage())) {
                this.finish();
                return;
            }
        }

        this.resetUI();

        this.parseAllBoards();

        this.mAdapter = new BoardsListAdapter(this);
        this.updateVisibleBoards(this.mAdapter);
        this.setListAdapter(this.mAdapter);

        this.setTitle(this.getString(R.string.pick_board_title));
    }

    @Override
    protected void onStart() {
        super.onStart();

        SettingsEntity prevSettings = this.mCurrentSettings;
        this.mCurrentSettings = this.mSettings.getCurrentSettings();

        if (this.mCurrentSettings.theme != prevSettings.theme) {
            this.finish();
            Intent i = new Intent(this.getIntent());
            this.startActivity(i);
            return;
        }

        if (this.mCurrentSettings.isDisplayAllBoards != prevSettings.isDisplayAllBoards) {
            this.updateVisibleBoards(this.mAdapter);
        }
    }

    private void parseAllBoards() {
        //TODO: загружать /makaba/mobile.fcgi?task=get_boards
        this.mBoards.addAll(this.parseBoardsList(R.array.pickboard_boards));
    }

    private void updateVisibleBoards(BoardsListAdapter adapter) {
        adapter.clear();

        String currentGroup = null;
        for (BoardModel board : this.mBoards) {
            if (!board.isVisible && !this.mSettings.isDisplayAllBoards()) {
                continue; // ignore hidden boards
            }

            // add group header if necessary
            if (board.group != null && !board.group.equals(currentGroup)) {
                currentGroup = board.group;
                adapter.add(new SectionEntity(currentGroup));
            }

            // add item
            adapter.add(new BoardEntity(board.code, board.title));
        }

        // add favorite boards
        List<FavoritesEntity> favoriteBoards = this.mFavoritesDatasource.getFavoriteBoards();
        for (FavoritesEntity f : favoriteBoards) {
            String boardName = f.getBoard();
            adapter.addItemToFavoritesSection(boardName, this.findBoardByCode(boardName));
        }
    }

    private BoardModel findBoardByCode(String code) {
        for (BoardModel board : this.mBoards) {
            if (board.code.equals(code)) {
                return board;
            }
        }

        return null;
    }

    private ArrayList<BoardModel> parseBoardsList(int arrayId) {
        ArrayList<BoardModel> boards = new ArrayList<BoardModel>();

        String[] entities = this.getResources().getStringArray(arrayId);
        String currentGroup = null;
        for (String entity : entities) {
            String[] parts = entity.split(";\\s?");
            if (parts.length == 1) {
                currentGroup = parts[0];
            } else if (parts.length >= 2) {
                boards.add(new BoardModel(parts[0], parts[1], true, currentGroup));
            }
        }

        return boards;
    }

    private void resetUI() {
        this.setTheme(this.mSettings.getTheme());
        this.setContentView(R.layout.pick_board_view);

        this.registerForContextMenu(this.getListView());

        final Button pickBoardButton = (Button) this.findViewById(R.id.pick_board_button);
        final EditText pickBoardInput = (EditText) this.findViewById(R.id.pick_board_input);

        pickBoardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredBoard = pickBoardInput.getText().toString().trim();
                PickBoardActivity.this.checkAndNavigateBoard(enteredBoard);
            }
        });

        pickBoardInput.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String enteredBoard = pickBoardInput.getText().toString().trim();
                    PickBoardActivity.this.checkAndNavigateBoard(enteredBoard);
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        BoardEntity item = (BoardEntity) this.mAdapter.getItem(position);
        this.checkAndNavigateBoard(item.getCode());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        BoardEntity item = (BoardEntity) this.mAdapter.getItem(info.position);

        menu.add(Menu.NONE, Constants.CONTEXT_MENU_COPY_URL, 0, this.getString(R.string.cmenu_copy_url));

        if (!this.mFavoritesDatasource.hasFavorites(this.mWebsite.name(), item.getCode(), null)) {
            menu.add(Menu.NONE, Constants.CONTEXT_MENU_ADD_FAVORITES, 0, this.getString(R.string.cmenu_add_to_favorites));
        } else {
            menu.add(Menu.NONE, Constants.CONTEXT_MENU_REMOVE_FAVORITES, 0, this.getString(R.string.cmenu_remove_from_favorites));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        BoardEntity model = (BoardEntity) this.mAdapter.getItem(menuInfo.position);

        switch (item.getItemId()) {
            case Constants.CONTEXT_MENU_COPY_URL: {
                String uri = this.mUrlBuilder.getPageUrlHtml(model.getCode(), 0);

                CompatibilityUtils.copyText(this, uri, uri);

                AppearanceUtils.showToastMessage(this, uri);
                return true;
            }
            case Constants.CONTEXT_MENU_ADD_FAVORITES: {
                this.addToFavorites(model.getCode());
                return true;
            }
            case Constants.CONTEXT_MENU_REMOVE_FAVORITES: {
                this.removeFromFavorites(model);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.pickboard, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add_board_id:
                final EditTextDialog dialog = new EditTextDialog(this);
                dialog.setTitle(this.getString(R.string.menu_add_favorites));
                dialog.setHint(this.getString(R.string.pick_board_input_hint));

                dialog.setPositiveButtonListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String boardCode = dialog.getText();
                        boardCode = fixSlashes(boardCode);
                        boolean success = PickBoardActivity.this.validateBoardCode(boardCode);

                        if (success) {
                            PickBoardActivity.this.addToFavorites(boardCode);
                            dialog.dismiss();
                        } else {
                            AppearanceUtils.showToastMessage(PickBoardActivity.this, PickBoardActivity.this.getString(R.string.warning_enter_board));
                        }
                    }
                });

                dialog.show();
                break;
            case R.id.preferences_menu_id:
                Intent preferencesIntent = new Intent(this.getApplicationContext(), ApplicationPreferencesActivity.class);
                this.startActivity(preferencesIntent);
                break;
            case R.id.tabs_menu_id:
                Intent openTabsIntent = new Intent(this.getApplicationContext(),
                        Constants.SDK_VERSION >= 4 ? TabsHistoryBookmarksActivity.class : TabsHistoryBookmarksCompActivity.class);
                this.startActivity(openTabsIntent);
                break;
        }

        return true;
    }

    private void checkAndNavigateBoard(String boardCode) {
        boardCode = this.fixSlashes(boardCode);

        if (!navigateBoard(boardCode)) {
            AppearanceUtils.showToastMessage(this, this.getString(R.string.warning_enter_board));
            return;
        }
    }

    private boolean navigateBoard(String boardCode) {
        if (!this.validateBoardCode(boardCode)) {
            return false;
        }

        this.mNavigationService.navigateBoardPage(this, null, this.mWebsite.name(), boardCode, 0, false);
        return true;
    }

    private void addToFavorites(String boardCode) {
        this.mFavoritesDatasource.addToFavorites(this.mWebsite.name(), boardCode, null, null);
        this.mAdapter.addItemToFavoritesSection(boardCode, this.findBoardByCode(boardCode));
    }

    private void removeFromFavorites(BoardEntity model) {
        this.mFavoritesDatasource.removeFromFavorites(this.mWebsite.name(), model.getCode(), null);
        this.mAdapter.removeItemFromFavoritesSection(model);
    }

    private boolean validateBoardCode(String boardCode) {
        if (!StringUtils.isEmpty(boardCode) && boardCodePattern.matcher(boardCode).matches()) {
            return true;
        }

        return false;
    }

    private String fixSlashes(String boardCode) {
        String result = boardCode.replaceAll("/", "").toLowerCase(Locale.US);

        return result;
    }

}
