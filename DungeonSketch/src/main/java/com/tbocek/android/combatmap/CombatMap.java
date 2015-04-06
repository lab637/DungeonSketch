package com.tbocek.android.combatmap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.tbocek.android.combatmap.TokenDatabase.TagTreeNode;
import com.tbocek.android.combatmap.cast.CastManager;
import com.tbocek.android.combatmap.model.Grid;
import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.MapDrawer;
import com.tbocek.android.combatmap.model.MapDrawer.FogOfWarMode;
import com.tbocek.android.combatmap.model.MultiSelectManager;
import com.tbocek.android.combatmap.model.primitives.BackgroundImage;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.OnScreenText;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Text;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.tokenmanager.TokenManager;
import com.tbocek.android.combatmap.view.CombatView;
import com.tbocek.android.combatmap.view.Dice;
import com.tbocek.android.combatmap.view.DrawOptionsView;
import com.tbocek.android.combatmap.view.TagNavigator;
import com.tbocek.android.combatmap.view.TokenSelectorView;
import com.tbocek.dungeonsketch.BuildConfig;
import com.tbocek.dungeonsketch.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import static android.support.v7.view.ActionMode.Callback;
import static com.tbocek.android.combatmap.view.DrawOptionsView.OnChangeDrawToolListener;

/**
 * This is the main activity that allows the user to sketch a map, and place and
 * manipulate tokens. Most of the application logic that does not relate to
 * token management occurs in this activity or one of its views.
 * 
 * @author Tim Bocek
 */
@SuppressLint("CommitPrefEdits")  // These are committed in an extracted method that trips up lint.
public final class CombatMap extends ActionBarActivity {

    /**
     * Dialog ID to use for the save file dialog.
     */
    private static final int DIALOG_ID_SAVE = 0;

	/**
	 * Dialog ID to use for the draw text dialog.
	 */
	private static final int DIALOG_ID_DRAW_TEXT = 1;

    /**
     * Dialog ID to use when confirming a save file name, in case the name would
     * overwrite a different map file.
     */
    private static final int DIALOG_ID_SAVE_NAME_CONFIRM = 2;

    private static final int DIALOG_ID_GRID_PROPERTIES = 3;

	private static final int DIALOG_ID_EXPORT = 4;

    /**
     * Dialog ID to use when creating or editing an information location.
     */
    private static final int DIALOG_ID_CREATE_INFO_LOCATION = 5;
    private static final String TAG = "CombatMap";

    /**
	 * The current map.
	 */
	private static MapData mData;

	/**
	 * Identifier for the draw annotations mode.
	 */
	private static final int MODE_DRAW_ANNOTATIONS = 3;

	/**
	 * Identifier for the draw background mode.
	 */
	private static final int MODE_DRAW_BACKGROUND = 1;

	/**
	 * Identifier for the draw GM notes mode.
	 */
	private static final int MODE_DRAW_GM_NOTES = 4;

	/**
	 * Identifier for the manipulate tokens mode.
	 */
	private static final int MODE_TOKENS = 2;

	/**
	 * ID of the Intent request to pick a new background image.
	 */
	private static final int REQUEST_PICK_BACKGROUND_IMAGE = 0;

	/**
	 * The attempted save name used when an extra saved prompt is need
			}ed (i.e.
	 * when saving over a different map).
	 */
	private String mAttemptedMapName;

	/**
	 * This frame renders on the bottom of the screen to provide controls
	 * related to the current interaction mode, i.e. the token list or drawing
	 * tools.
	 */
	private FrameLayout mBottomControlFrame;

	/**
	 * The view that manages the main canvas for drawing and tokens.
	 */
	private CombatView mCombatView;

	/**
			}
	 * The view that allows the user to select a drawing tool or color.
	 */
	private DrawOptionsView mDrawOptionsView;

	/**
	 * The text object that the edit dialog is currently editing, or null if a
	 * new text object is being created.
	 */
	private Text mEditedTextObject;

	/**
	 * Whether the control tray on the bottom of the screen is expanded.
	 */
	private boolean mIsControlTrayExpanded = true;

	/**
	 * The action mode that was started to manage the selection.
	 */
	private ActionMode mActionMode;

	/**
	 * Location at which to place requested objects such as text or background
	 * images, in world space.
	 */
	private PointF mNewObjectLocationWorldSpace;
	
	private ToggleButton mMeasuringToggle;

    /**
     * The drawables that should be used for undo and redo IF the action is available.  These
     * change based on whether the theme is dark or not.
     */
    int mUndoMenuItemDrawable = R.drawable.undo;
    int mRedoMenuItemDrawable = R.drawable.redo;


	/**
	 * Listener that fires when a new draw tool or color has been selected.
	 */
	private final OnChangeDrawToolListener mOnChangeDrawToolListener = new OnChangeDrawToolListener() {

		@Override
		public void onChangeMaskEditing(boolean editingMask) {
			CombatMap.this.mCombatView.setEditingLayerMask(editingMask);
		}

		@Override
		public void onChooseCircleTool() {
            mSelectedToolTextView.setText("Draw Circle");
			CombatMap.this.mCombatView.setDrawMode();
			CombatMap.this.mCombatView
					.setNewLineStyle(CombatView.NewLineStyle.CIRCLE);
		}

		@Override
		public void onChooseColoredPen(final int color) {
			CombatMap.this.mCombatView.setNewLineColor(color);
		}

		@Override
		public void onChooseEraser() {
			mSelectedToolTextView.setText("Eraser");
            CombatMap.this.mCombatView.setEraseMode();
		}

		@Override
		public void onChooseFreeHandTool() {
            mSelectedToolTextView.setText("Draw Freehand Line");
			CombatMap.this.mCombatView.setDrawMode();
			CombatMap.this.mCombatView
					.setNewLineStyle(CombatView.NewLineStyle.FREEHAND);
		}
		
		@Override
		public void onChooseImageTool() {
			mSelectedToolTextView.setText("Add Background Image");
            CombatMap.this.mCombatView.setBackgroundImageMode();
		}

		@Override
		public void onChooseMaskEraser() {
            mSelectedToolTextView.setText("Delete Mask Region");
			CombatMap.this.mCombatView.setFogOfWarEraseMode();
		}

		@Override
		public void onChooseMaskTool() {
            mSelectedToolTextView.setText("Draw Mask");
			CombatMap.this.mCombatView.setFogOfWarDrawMode();
			CombatMap.this.mCombatView
					.setNewLineStyle(CombatView.NewLineStyle.FREEHAND);
		}

		@Override
		public void onChoosePanTool() {
            mSelectedToolTextView.setText("Zoom and Pan");
			CombatMap.this.mCombatView.setZoomPanMode();
		}

		@Override
		public void onChooseRectangleTool() {
            mSelectedToolTextView.setText("Draw Rectangle");
			CombatMap.this.mCombatView.setDrawMode();
			CombatMap.this.mCombatView
					.setNewLineStyle(CombatView.NewLineStyle.RECTANGLE);
		}

		@Override
		public void onChooseStraightLineTool() {
            mSelectedToolTextView.setText("Draw Straight Line");
			CombatMap.this.mCombatView.setDrawMode();
			CombatMap.this.mCombatView
					.setNewLineStyle(CombatView.NewLineStyle.STRAIGHT);
		}

		@Override
		public void onChooseStrokeWidth(final float width) {
			CombatMap.this.mCombatView.setNewLineStrokeWidth(width);
		}

		@Override
		public void onChooseTextTool() {
            mSelectedToolTextView.setText("Add Text To Map");
			CombatMap.this.mCombatView.setTextMode();

		}

        @Override
        public void onChooseInfoTool() {
            mSelectedToolTextView.setText("Add Information Point");
            CombatMap.this.mCombatView.setInfoMode();
        }

        @Override
        public void onChooseSelectionTool() {
            mSelectedToolTextView.setText("Select Region");
            CombatMap.this.mCombatView.setSelectMode();
        }

        @Override
		public void onChooseMoveTokenTool() {
            mSelectedToolTextView.setText("Move Tokens");
			CombatMap.this.mCombatView.setTokenManipulationMode();
		}
	};

	/**
	 * Callback to listen for text edit/creation requests and load the required
	 * dialog, since dialogs need to be managed at the activity level.
	 */
	private final CombatView.ActivityRequestListener mOnNewTextEntryListener =
            new CombatView.ActivityRequestListener() {
		@Override
		public void requestEditTextObject(OnScreenText t) {
			CombatMap.this.mEditedTextObject = t;
			CombatMap.this.showDialog(DIALOG_ID_DRAW_TEXT);
		}

		@Override
		public void requestNewTextEntry(PointF newTextLocationWorldSpace) {
			CombatMap.this.mEditedTextObject = null;
			CombatMap.this.mNewObjectLocationWorldSpace = newTextLocationWorldSpace;
			CombatMap.this.showDialog(DIALOG_ID_DRAW_TEXT);
		}

		@Override
		public void requestNewBackgroundImage(PointF locationWorldSpace) {
			CombatMap.this.mNewObjectLocationWorldSpace = locationWorldSpace;
			CombatMap.this.startActivityForResult(new Intent(
					Intent.ACTION_PICK,
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
					REQUEST_PICK_BACKGROUND_IMAGE);
		}

        @Override
        public void requestNewInfoEntry(PointF locationWorldSpace) {
            CombatMap.this.mEditedTextObject = null;
            CombatMap.this.mNewObjectLocationWorldSpace = locationWorldSpace;
            CombatMap.this.showDialog(DIALOG_ID_CREATE_INFO_LOCATION);
        }

        @Override
        public void requestEditInfoObject(Information information) {
            CombatMap.this.mEditedTextObject = information;
            CombatMap.this.showDialog(DIALOG_ID_CREATE_INFO_LOCATION);
        }

        @Override
        public void requestRegionSelected() {
            mActionMode = getSupportActionBar().startActionMode(new LineSelectionActionModeCallback());

            int manipulationMode = mSharedPreferences.getInt(
                    "manipulation_mode", MODE_TOKENS);
            boolean shouldSnap = mSharedPreferences.getBoolean(
                    getModeSpecificSnapPreferenceName(manipulationMode), true);
            mActionMode.getMenu().findItem(R.id.menu_snap_to_grid_context).setChecked(shouldSnap);
        }
    };
	
	private final TagNavigator.TagSelectedListener mTagSelectedListener = new TagNavigator.TagSelectedListener() {
		
		@Override
		public void onTagSelected(TagTreeNode selectedTag) {
			CombatMap.this.mTokenSelector.setSelectedTag(selectedTag.getPath(),
					CombatMap.this.mCombatView);
			if (mData != null && selectedTag.getParent() != null) {
				mData.setLastTag(selectedTag.getPath());
			}

            mDeployTokensButton.setVisibility(selectedTag.getParent() != null
                    ? View.VISIBLE : View.GONE);
		}

		@Override
		public void onDragTokensToTag(Collection<BaseToken> token,
				TagTreeNode tag) { }
	};

	/**
	 * Listener that fires when an image has been selected in the CombatView.
	 */
	private final CombatView.ImageSelectionListener mOnImageSelectListener =
            new CombatView.ImageSelectionListener() {

		@Override
		public void onSelectNoBackgroundImage() {
			if (CombatMap.this.mActionMode != null) {
				mActionMode.finish();
				mActionMode = null;
			}
		}

		@Override
		public void onSelectBackgroundImage(BackgroundImage selectedImage) {
			if (mActionMode == null) {
				CombatMap.this.mActionMode = CombatMap.this
						.startSupportActionMode(new ImageSelectionActionModeCallback());
			}

			Menu m = CombatMap.this.mActionMode.getMenu();
			m.findItem(R.id.background_image_maintain_aspect_ratio).setChecked(
					selectedImage.shouldMaintainAspectRatio());
		}
	};

	/**
	 * Listener that fires when a token has been selected in the token selector
	 * view.
	 */
	private final TokenSelectorView.OnTokenSelectedListener mOnTokenSelectedListener = new TokenSelectorView.OnTokenSelectedListener() {
		@Override
		public void onTokenSelected(final BaseToken t) {
			CombatMap.this.mCombatView.placeToken(t);
		}
	};

	/**
	 * This view provides an area to render controls in a region that draws over
	 * the main canvas and can be displayed or hidden as needed. Currently used
	 * to draw the token category selector.
	 */
	private View mPopupFrame;

	/**
	 * The saved menu item that performs the redo operation.
	 */
	private MenuItem mRedoMenuItem;


    /**
     * The saved "save" menu item; this is cached because its appearence needs to change base on
     * grid theme.
     */
    private MenuItem mSaveMenuItem;

    private MenuItem mViewMenuItem;

	/**
	 * Cached SharedPreferences.
	 */
	private SharedPreferences mSharedPreferences;

	/**
	 * The menu item that controls whether drawing/tokens snap to the grid.
	 * Saved because we need to listen for these events.
	 */
	private MenuItem mSnapToGridMenuItem;

    private MenuItem mRestoreViewMenuItem;

    private MenuItem mCastViewMenuItem;

    private MenuItem mStopCastingViewMenuItem;

	/**
	 * Object to manage which mode is changed to when a new tab is selected.
	 */
	private TabManager mTabManager;

	/**
	 * Callback that loads the correct interaction mode when a new tab is
	 * selected.
	 */
	private final TabManager.TabSelectedListener mTabSelectedListener =
            new TabManager.TabSelectedListener() {
		@Override
		public void onTabSelected(int tab) {
			if (mData != null) {
				CombatMap.this.setManipulationMode(tab);
			}
		}
	};

	/**
	 * Whether the tag selector is visible.
	 */
	private boolean mTagSelectorVisible;
	
	private TagNavigator mTagNavigator;

	/**
	 * Database of available combat tokens.
	 */
	private TokenDatabase mTokenDatabase;

	/**
	 * The view that allows the user to select a token for the map.
	 */
	private TokenSelectorView mTokenSelector;

	/**
	 * The saved menu item that performs the undo operation.
	 */
	private MenuItem mUndoMenuItem;

    private Button mDeployTokensButton;
    private TokenImageManager.Loader mLoader;

    private TextView mSelectedToolTextView;

    private CastManager mCastManager;
    private int mOldBottomControlFrameHeight;

    private int lastUsedTheme = -1;
    private ToggleButton mDiceToggle;
    private Dice mDice;

    /**
	 * Given a combat mode, returns the snap to grid preference name associated
	 * with that combat mode.
	 * 
	 * @param mode
	 *            The combat mode to check.
	 * @return Name of the snap preference associated with that combat mode.
	 */
	private String getModeSpecificSnapPreferenceName(final int mode) {
		return mode == MODE_TOKENS ? "snaptokens" : "snapdrawing";
	}

	/**
	 * Loads the map with the given name (no extension), and replaces the
	 * currently loaded map with it.
	 * 
	 * @param name
	 *            Name of the map to load.
	 */
	public void loadMap(final String name) {
        MapLoadUtils.loadMap(this, name, null);
        setTitle();
		mData = MapData.getInstance();
        if (mCombatView != null) {
            this.mCombatView.setData(mData);
        }

        if (mTagNavigator != null) {
            this.mTagNavigator.setTagPath(mData.getLastTag());
        }
	}



    @Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		// If an image was successfully picked, use it.
		if (requestCode == REQUEST_PICK_BACKGROUND_IMAGE) {
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				DataManager dm = new DataManager(this.getApplicationContext());
				try {
					String newFileName = dm.copyToMapDataFiles(selectedImage);
					BackgroundImage image = mData.getBackgroundImages().addImage(newFileName,
							this.mNewObjectLocationWorldSpace);
                    mData.getBackgroundImages().loadImage(this, image, new Runnable() {
                        @Override
                        public void run() {
                            mCombatView.refreshMap();
                        }
                    });
					this.mCombatView.refreshMap();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Loads the preference that controls what the current manipulation mode is.
	 */
	private void loadModePreference() {
		// Set the current mode to the selected mode.
		this.setManipulationMode(this.mSharedPreferences.getInt(
				"manipulation_mode", MODE_DRAW_BACKGROUND));
	}

	/**
	 * Loads the snap preference associated with the current combat map mode.
	 */
	private void loadModeSpecificSnapPreference() {
		int manipulationMode = this.mSharedPreferences.getInt(
				"manipulation_mode", MODE_TOKENS);

		boolean shouldSnap = this.mSharedPreferences.getBoolean(
				this.getModeSpecificSnapPreferenceName(manipulationMode), true);

		this.mCombatView.setShouldSnapToGrid(shouldSnap);
		this.mCombatView.setTokensSnapToIntersections(this.mSharedPreferences
                .getBoolean("tokenssnaptogridlines", false));

		if (this.mSnapToGridMenuItem != null) {
			this.mSnapToGridMenuItem.setChecked(shouldSnap);
		}
	}

	/**
	 * Attempts to load map data, or creates a new map if this fails.
	 */
	private void loadOrCreateMap() {
		if (MapData.hasValidInstance()) {
			mData = MapData.getInstance();
            if (this.mCombatView != null) {
                this.mCombatView.setData(mData);
            }
		} else {
			this.loadMap(DataManager.TEMP_MAP_NAME);
		}
        mData.getBackgroundImages().loadImages(this, new Runnable() {
            @Override
            public void run() {
                mCombatView.refreshMap();
            }
        });
		this.setUndoRedoEnabled();

	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
        // Fix spurious strict mode errors that fire when recreating the activity.
        if (BuildConfig.DEBUG)
        {
            System.gc();
        }
        DeveloperMode.strictMode();

        Units.initialize(this.getApplicationContext());

        this.mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        this.loadOrCreateMap();

        setCorrectTheme();

		// android.os.Debug.startMethodTracing("main_activity_load");
		super.onCreate(savedInstanceState);

        maybeShowImportPromo();

        if (this.getApplicationContext() == null) return;

		BackgroundImage.registerDataManager(new DataManager(this
				.getApplicationContext()));

        mCastManager = CastManager.getInstance(this);
        mCastManager.setCallback(mCastCallback);
        mCastManager.onCreate();

		initializeUi();
	}
	
	//this is called when the screen rotates.
	// (onCreate is no longer called when screen rotates due to manifest, see: android:configChanges)
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
	    super.onConfigurationChanged(newConfig);
	    setContentView(R.layout.combat_map_layout);
	    initializeUi();
	}
	
	private void initializeUi() {
        // TODO: get this off the main thread
        Information.loadInfoBitmap(this);

        // Set up the tabs
		this.setContentView(R.layout.combat_map_layout);
		ActionBar actionBar = this.getSupportActionBar();
		this.mTabManager = new TabManager(actionBar, this);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// Clear the title on the action bar, since we want to leave more
		// space for the tabs.
		actionBar.setTitle("");

		if (mCombatView == null) {
			this.mCombatView = new CombatView(this);
            this.mCombatView.setData(mData);
		}
		this.registerForContextMenu(this.mCombatView);
		this.mCombatView.setNewTextEntryListener(this.mOnNewTextEntryListener);
		this.mCombatView.setImageSelectionListener(this.mOnImageSelectListener);

		this.mTokenSelector = new TokenSelectorView(
				this.getApplicationContext());

        mLoader = TokenImageManager.getInstance().createLoader(this, new Handler());
        mLoader.start();
        mLoader.getLooper(); // Make sure loader thread is ready to go.
        mTokenSelector.setLoader(mLoader);
        mCombatView.setLoader(mLoader);

        // Set up listeners for the token selector's category and manager
		// buttons.
		this.mTokenSelector
				.setOnTokenSelectedListener(this.mOnTokenSelectedListener);

		this.mTokenSelector
				.setOnClickGroupSelectorListener(new View.OnClickListener() {

					@Override
					public void onClick(final View arg0) {
						CombatMap.this
								.setTagSelectorVisibility(!CombatMap.this.mTagSelectorVisible);
					}
				});

		this.mDrawOptionsView = new DrawOptionsView(
				this.getApplicationContext());
		this.mDrawOptionsView
				.setOnChangeDrawToolListener(this.mOnChangeDrawToolListener);

		FrameLayout mainContentFrame = (FrameLayout) this
				.findViewById(R.id.mainContentFrame);
		this.mBottomControlFrame = (FrameLayout) this
				.findViewById(R.id.bottomControlAreaFrame);
		this.mPopupFrame = this
				.findViewById(R.id.popupControlAreaFrame);
        FrameLayout innerPopupFrame = (FrameLayout) this
                .findViewById(R.id.popupControlAreaInnerFrame);
        this.mSelectedToolTextView = (TextView) this.findViewById(R.id.selectedToolText);
		
		this.mTagNavigator = new TagNavigator(this);
		this.mTagNavigator.setLayoutParams(new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT));
		this.mTagNavigator.setBackgroundDrawable(new ColorDrawable(Color.argb(60, 100, 100, 100)));
        this.mTagNavigator.setTagSelectedListener(this.mTagSelectedListener);


		//this.mPopupFrame.addView(this.mTokenCategorySelector);
		innerPopupFrame.addView(this.mTagNavigator);
		
		mainContentFrame.addView(this.mCombatView);
		this.mBottomControlFrame.addView(this.mTokenSelector);

		final ImageButton collapseButton = (ImageButton) this
				.findViewById(R.id.bottomControlAreaExpandButton);
		collapseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View arg0) {
				CombatMap.this.mIsControlTrayExpanded = !CombatMap.this.mIsControlTrayExpanded;
				if (CombatMap.this.mIsControlTrayExpanded) {
					CombatMap.this.mBottomControlFrame.getLayoutParams().height =
                            mOldBottomControlFrameHeight;
					collapseButton
							.setImageResource(R.drawable.vertical_contract);
				} else {
                    mOldBottomControlFrameHeight =
                            CombatMap.this.mBottomControlFrame.getLayoutParams().height;
					CombatMap.this.mBottomControlFrame.getLayoutParams().height = 0;
					collapseButton.setImageResource(R.drawable.vertical_expand);
				}
				CombatMap.this.findViewById(R.id.combatMapMainLayout)
						.requestLayout();
			}
		});
		
		this.mMeasuringToggle = (ToggleButton) this.findViewById(R.id.combat_map_toggle_measuring_tape);
		this.mMeasuringToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ToggleButton tb = (ToggleButton)v;
				if (tb.isChecked()) {
					mCombatView.setMeasuringTapeMode();
				} else {
					mCombatView.setTokenManipulationMode();
				}
			}
		});

        this.mDice = (Dice) this.findViewById(R.id.combat_map_dice);
        this.mDiceToggle = (ToggleButton) this.findViewById(R.id.combat_map_toggle_dice);
        this.mDiceToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleButton tb = (ToggleButton)v;
                if (tb.isChecked()) {
                    mDice.setVisibility(View.VISIBLE);
                } else {
                    mDice.setVisibility(View.GONE);
                }
            }
        });
		
		this.mDeployTokensButton = (Button) this.findViewById(R.id.deployTokensButton);
		mDeployTokensButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openDeployTokensDialog();
            }
        });

		if (this.mTabManager != null) {
			this.mTabManager.addTab(this.getString(R.string.background),
					MODE_DRAW_BACKGROUND, true);
			this.mTabManager.addTab(this.getString(R.string.gm_notes),
					MODE_DRAW_GM_NOTES, true);
			this.mTabManager.addTab(this.getString(R.string.combat),
					MODE_TOKENS, false);
			this.mTabManager.addTab(this.getString(R.string.annotations),
					MODE_DRAW_ANNOTATIONS, false);
			this.mTabManager.setTabSelectedListener(this.mTabSelectedListener);
		}

		this.reloadPreferences();

		this.mCombatView
				.setOnRefreshListener(new CombatView.OnRefreshListener() {
					@Override
					public void onRefresh(boolean interactionDone) {
						// When the map is refreshed, update the undo/redo
						// status as
						// well.
						CombatMap.this.setUndoRedoEnabled();

                        // When the map is refreshed, if we are connected to Chromecast export the
                        // token layer (and *only* the token layer) to Chromecast.
                        if (interactionDone) {
                            CombatMap.this.exportToChromecast();
                        }
					}
                });

		this.mCombatView.getMultiSelect().setSelectionChangedListener(
				new SelectionChangedListener());

		this.mCombatView.alertTokensChanged();
		this.mCombatView.requestFocus();

        this.setElevations();

        this.styleActionBar();

	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setElevations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBottomControlFrame.setElevation(5.0f);
            mTagNavigator.setElevation(5.0f);
            mMeasuringToggle.setElevation(5.0f);
            this.findViewById(R.id.bottomControlAreaExpandButton).setElevation(5.0f);
            mCombatView.setElevation(-1);
        }
    }

    private void exportToChromecast() {
        if (mCastManager.isCasting()) {
            // Create a 1080p 16x9 bitmap
            Bitmap b = mCastManager.getCastBuffer();
            Canvas canvas = new Canvas(b);

            new MapDrawer()
                    .drawGridLines(true)
                    .drawGmNotes(false)
                    .drawTokens(true)
                    .areTokensManipulable(true)
                    .drawAnnotations(true)
                    .backgroundFogOfWar(FogOfWarMode.CLIP)
                    .useCustomWorldSpaceTransformer(mData.getChromecastWorldSpaceTransformer())
                    .draw(canvas, mData, canvas.getClipBounds());

            try {
                mCastManager.updateImage(b);
            } catch (IOException e) {
                Log.w(TAG, "Error updating Chromecast image", e);
            }
        }
    }

    @Override
	public Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_ID_SAVE:
			return new TextPromptDialog(this,
					new TextPromptDialog.OnTextConfirmedListener() {
						@Override
						public void onTextConfirmed(final String text) {
							// If the save file name exists and is not the
							// current file,
							// warn about overwriting.
							if (!text.equals(CombatMap.this.mSharedPreferences
									.getString("filename", ""))
									&& new DataManager(CombatMap.this
											.getApplicationContext())
											.saveFileExists(text)) {
								CombatMap.this.mAttemptedMapName = text;
								CombatMap.this
										.showDialog(CombatMap.DIALOG_ID_SAVE_NAME_CONFIRM);
							} else {
								CombatMap.this.setFilenamePreference(text);
                                SaverService.startSavingMap(getApplicationContext(), text);
							}
						}
					}, this.getString(R.string.save_map), this
							.getString(R.string.save));
		case DIALOG_ID_DRAW_TEXT:

			return new FontDialog(this,
					new FontDialog.OnTextConfirmedListener() {
						@Override
						public void onTextConfirmed(final String text,
								final float size) {
							if (CombatMap.this.mEditedTextObject == null) {
								CombatMap.this.mCombatView
										.createNewText(
												CombatMap.this.mNewObjectLocationWorldSpace,
												text, size);
							} else {
								CombatMap.this.mCombatView
										.getActiveLines()
										.editText(
                                                (OnScreenText) CombatMap.this.mEditedTextObject,
												text,
												size,
												CombatMap.this.mCombatView
														.getWorldSpaceTransformer());
								CombatMap.this.mCombatView.refreshMap();
							}
						}
					});
            case DIALOG_ID_CREATE_INFO_LOCATION:

                return new InfoPointDialog(this,
                        new InfoPointDialog.OnTextConfirmedListener() {
                            @Override
                            public void onTextConfirmed(final String text, int iconId) {
                                if (CombatMap.this.mEditedTextObject == null) {
                                    CombatMap.this.mCombatView
                                            .createNewInfo(
                                                    CombatMap.this.mNewObjectLocationWorldSpace,
                                                    text, iconId);
                                } else {
                                    CombatMap.this.mCombatView
                                            .getActiveLines()
                                            .editInfo(
                                                    (Information) CombatMap.this.mEditedTextObject,
                                                    text,
                                                    iconId
                                            );
                                    CombatMap.this.mCombatView.refreshMap();
                                }
                            }
                        });
		case DIALOG_ID_SAVE_NAME_CONFIRM:
			return new AlertDialog.Builder(CombatMap.this)
					.setMessage("Map already exists.  Save over it?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									CombatMap.this
											.setFilenamePreference(CombatMap.this.mAttemptedMapName);
                                    SaverService.startSavingMap(getApplicationContext(), mAttemptedMapName);
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									CombatMap.this.mAttemptedMapName = null;
								}
							}).create();
		case DIALOG_ID_GRID_PROPERTIES:
			GridPropertiesDialog gpd = new GridPropertiesDialog(this);
			gpd.setOnPropertiesChangedListener(new GridPropertiesDialog.PropertiesChangedListener() {
				@Override
				public void onPropertiesChanged() {
                    setCorrectTheme();
                    recreate();
                }

                @Override
                public void onResizeGridRequested() {
                    mCombatView.setResizeGridMode();
                    mBottomControlFrame.removeAllViews();
                }
            });
			return gpd;
		case DIALOG_ID_EXPORT:
			return new ExportImageDialog(this);
		default:
			return null;
		}
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setCorrectTheme() {
        if (mData.getGrid().getColorScheme().isDark()) {
            if (mData.getGrid().getColorScheme().isSecondaryDark()) {
                this.setTheme(R.style.MapStyleDarkGrid);
            } else {
               this.setTheme(R.style.MapStyleDarkGridLightSecondary);
            }
        } else {
            if (mData.getGrid().getColorScheme().isSecondaryDark()) {
                this.setTheme(R.style.MapStyleLightGridDarkSecondary);
            } else {
                this.setTheme(R.style.MapStyleLightGrid);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void styleActionBar() {

        this.getSupportActionBar().setStackedBackgroundDrawable( new ColorDrawable(
                mData.getGrid().getColorScheme().getLineColor()));
        this.getSupportActionBar().setBackgroundDrawable( new ColorDrawable(
                mData.getGrid().getColorScheme().getBackgroundColor()));

        // On Lollipop, set the status bar color as well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create a color that is slightly darker.
            float[] hsv = new float[3];
            Color.colorToHSV(mData.getGrid().getColorScheme().getBackgroundColor(), hsv);
            hsv[2] *= .75;
            getWindow().setStatusBarColor(Color.HSVToColor(hsv));
        }

        mUndoMenuItemDrawable = mData.getGrid().getColorScheme().isDark()
                ? R.drawable.undo : R.drawable.undo_dark;
        mRedoMenuItemDrawable = mData.getGrid().getColorScheme().isDark()
                ? R.drawable.redo : R.drawable.redo_dark;
        setUndoRedoEnabled();   // Refresh the undo/redo icons.

        if (mSaveMenuItem != null) {
            mSaveMenuItem.setIcon(mData.getGrid().getColorScheme().isDark()
                    ? R.drawable.document_save : R.drawable.document_save_dark);
        }

        if (mViewMenuItem != null) {
            mViewMenuItem.setTitle(mViewMenuItem.getTitle().toString());
        }
        setTitle(); // Called to update color.

    }

    private void setTitle() {
        if (this.mSharedPreferences == null) return;

        String filename = this.mSharedPreferences.getString("filename", "");
        if (filename == null || filename.isEmpty()) {
            filename = "Untitled Map";
        }

        if (this.getSupportActionBar() == null) return;
        this.getSupportActionBar().setTitle(filename);
    }

    @Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.combat_map_menu, menu);

		this.mSnapToGridMenuItem = menu.findItem(R.id.menu_snap_to_grid);
        this.mRestoreViewMenuItem = menu.findItem(R.id.menu_restore_view);
        this.mCastViewMenuItem = menu.findItem(R.id.menu_cast_view);
        this.mStopCastingViewMenuItem = menu.findItem(R.id.menu_stop_casting_view);

        this.mViewMenuItem = menu.findItem(R.id.menu_view);
        mViewMenuItem.setTitle(mViewMenuItem.getTitle().toString());

		this.loadModeSpecificSnapPreference();

		this.mUndoMenuItem = menu.findItem(R.id.menu_undo);
		this.mRedoMenuItem = menu.findItem(R.id.menu_redo);
		this.setUndoRedoEnabled();

        this.mSaveMenuItem = menu.findItem(R.id.menu_save);
        mSaveMenuItem.setIcon(mData.getGrid().getColorScheme().isDark()
                ? R.drawable.document_save : R.drawable.document_save_dark);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mCastManager.getMediaRouteSelector());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_clear_all) {
			// Save the current map, if autosave was requested.
			if (this.mSharedPreferences.getBoolean("autosave", true)) {
				SaverService.startSavingMap(getApplicationContext(),
                        this.mSharedPreferences.getString("filename", ""));
			}
            this.setFilenamePreference(null);
			Grid g = mData.getGrid();
			MapData.clear();
			mData = MapData.getInstance();
			// Make sure the new map data has the same grid.
			mData.setGrid(g);
			this.mCombatView.setData(mData);
			this.reloadPreferences();
			return true;
		} else if (itemId == R.id.menu_settings) {
			this.startActivity(new Intent(this, Settings.class));
			return true;
		} else if (itemId == R.id.menu_snap_to_grid) {
			this.setModeSpecificSnapPreference(!this.mSnapToGridMenuItem
					.isChecked());
			return true;
		} else if (itemId == R.id.menu_save) {
			this.showDialog(DIALOG_ID_SAVE);
			return true;
		} else if (itemId == R.id.menu_load) {
			this.startActivity(new Intent(this, Load.class));
			return true;
		} else if (itemId == R.id.menu_undo) {
			this.mCombatView.getUndoRedoTarget().undo();
			this.mCombatView.alertTokensChanged();
			return true;
		} else if (itemId == R.id.menu_redo) {
			this.mCombatView.getUndoRedoTarget().redo();
			this.mCombatView.alertTokensChanged();
			return true;
		} else if (itemId == R.id.menu_grid_properties) {
			this.showDialog(DIALOG_ID_GRID_PROPERTIES);
			return true;
		} else if (itemId == R.id.menu_export) {
			this.showDialog(DIALOG_ID_EXPORT);
			return true;
		} else if (itemId == R.id.menu_help) {
			Help.openHelp(this);
			return true;
		} else if (itemId == R.id.menu_token_database) {
			Debug.startMethodTracing("tokenmanager");
			this.startActivity(new Intent(CombatMap.this, TokenManager.class));
			return true;
		} else if (itemId == R.id.menu_save_view) {
            mData.saveView();
            mRestoreViewMenuItem.setVisible(true);
            return true;
        } else if (itemId == R.id.menu_restore_view) {
            mData.restoreView();
            mCombatView.refreshMap();
            return true;
        } else if (itemId == R.id.menu_cast_view) {
            mData.castView();
            mCombatView.refreshMap();
            mCastViewMenuItem.setVisible(false);
            mStopCastingViewMenuItem.setVisible(true);
            return true;
        } else if (itemId == R.id.menu_stop_casting_view) {
            mData.stopCastingView();
            mCombatView.refreshMap();
            mCastViewMenuItem.setVisible(true);
            mStopCastingViewMenuItem.setVisible(false);
            return true;
        } else if (itemId == R.id.menu_zoom_to_fit) {
            mData.getWorldSpaceTransformer().zoomToFit(
                    mData.getBoundingRectangle(),
                    getWindowManager().getDefaultDisplay().getWidth(),
                    getWindowManager().getDefaultDisplay().getHeight());
            mCombatView.refreshMap();
            return true;
        } else {
			return false;
		}
	}

	@Override
	public void onPause() {
        if (isFinishing()) {
            mCastManager.detachCallbacks();
        }
		Editor editor = this.mSharedPreferences.edit();
        savePrefChanges(editor);
		String filename = this.mSharedPreferences.getString("filename", null);
		if (filename == null
				|| !this.mSharedPreferences.getBoolean("autosave", true)) {
			filename = DataManager.TEMP_MAP_NAME;
		}
		this.mCombatView.getMultiSelect().selectNone();

        SaverService.startSavingMap(getApplicationContext(), filename);
        mLoader.clearQueue();
        TokenImageManager.getInstance().recycleAll();
        super.onPause();
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLoader.quit();
    }

	@Override
	protected void onPrepareDialog(final int id, @Nonnull final Dialog dialog) {
		switch (id) {
		case DIALOG_ID_SAVE:
			// Attempt to load map data. If we can't load map data, create a
			// new map.
			String filename = this.mSharedPreferences.getString("filename", "");
			if (filename == null || filename.equals(DataManager.TEMP_MAP_NAME)) {
				filename = "";
			}
			TextPromptDialog d = (TextPromptDialog) dialog;
			d.fillText(filename);
			break;
		case DIALOG_ID_DRAW_TEXT:
			FontDialog fd = (FontDialog) dialog;
			if (this.mEditedTextObject != null) {

				fd.populateFields(this.mEditedTextObject.getText(),
                        ((OnScreenText)this.mEditedTextObject).getTextSize());
			} else {
				fd.clearText();
			}
			break;
        case DIALOG_ID_CREATE_INFO_LOCATION:
          InfoPointDialog fd2 = (InfoPointDialog) dialog;
            if (this.mEditedTextObject != null) {
                fd2.populateFields(this.mEditedTextObject.getText(), ((Information)this.mEditedTextObject).getIcon());
            } else {
                fd2.clearText();
            }
		    break;
        case DIALOG_ID_SAVE_NAME_CONFIRM:
			AlertDialog ad = (AlertDialog) dialog;
			ad.setMessage("There is already a map named \""
					+ this.mAttemptedMapName + "\".  Save over it?");
			break;
		case DIALOG_ID_GRID_PROPERTIES:
			GridPropertiesDialog gpd = (GridPropertiesDialog) dialog;
			gpd.setMapData(mData);
			break;
		case DIALOG_ID_EXPORT:
			// Attempt to load map data. If we can't load map data, create a
			// new map.
			filename = this.mSharedPreferences.getString("filename", "");
			if (filename == null || filename.equals(DataManager.TEMP_MAP_NAME)) {
				filename = "";
			}
			ExportImageDialog ed = (ExportImageDialog) dialog;
			ed.prepare(filename, mData, this.mCombatView.getWidth(),
					this.mCombatView.getHeight());
		default:
			super.onPrepareDialog(id, dialog);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

        mCastManager.attachCallbacks();

		this.loadOrCreateMap();

		this.reloadPreferences();

		this.mCombatView.refreshMap();
		new TokenDatabaseLoadTask().execute();
        setTitle();
		// android.os.Debug.stopMethodTracing();
	}

    private void maybeShowImportPromo() {
        boolean shown = mSharedPreferences.getBoolean("shown_import_promo", false);
        if (shown) return;

        if (ImportDataDialog.hasOtherVersion(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.import_data_alert_message)
                    .setTitle(R.string.import_data_alert_title)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(CombatMap.this, ImportDataDialog.class));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
                    }).show();
        }

        mSharedPreferences.edit().putBoolean("shown_import_promo", true).commit();
    }

    /**this.mCombatView.setTokenManipulationMode();
	 * Modifies the current map data according to any preferences the user has
	 * set.
	 */
	private void reloadPreferences() {
		this.mTokenSelector.setShouldDrawDark(mData.getGrid().isDark());
        this.mDice.setIsBackgroundDark(mData.getGrid().isDark());

		if (this.mTabManager != null) {
			this.mTabManager.pickTab(this.mSharedPreferences.getInt(
					"manipulation_mode", MODE_DRAW_BACKGROUND));
		}

		// We defer loading the manipulation mode until now, so that the correct
		// item is disabled after the menu is loaded.
		this.loadModePreference();

	}

	/**
	 * Sets the preference that will persist the name of the active file between
	 * sessions.
	 * 
	 * @param newFilename
	 *            The filename to set.
	 */
	private void setFilenamePreference(final String newFilename) {
		MapLoadUtils.setFilenamePreference(this, newFilename);
        setTitle();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void savePrefChanges(Editor editor) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    /**
	 * Sets the manipulation mode to the given mode.
	 * 
	 * @param manipulationMode
	 *            The mode to set to; should be a MODE_ constant declared in
	 *            this class.
	 */
	private void setManipulationMode(final int manipulationMode) {
		Editor editor = this.mSharedPreferences.edit();
		editor.putInt("manipulation_mode", manipulationMode);
		savePrefChanges(editor);

		switch (manipulationMode) {
		case MODE_DRAW_BACKGROUND:
			this.mCombatView.getMultiSelect().selectNone();
			this.mCombatView.setAreTokensManipulable(false);
			this.mCombatView.useBackgroundLayer();
			this.mCombatView.setFogOfWarMode(FogOfWarMode.DRAW);
			this.mBottomControlFrame.removeAllViews();
			this.mBottomControlFrame.addView(this.mDrawOptionsView);
			this.setModePreference(manipulationMode);
			this.mDrawOptionsView.setDefault();
			this.mDrawOptionsView.setMaskToolVisibility(true);
            this.mDrawOptionsView.setInformationButtonVisibility(false);
			this.mDrawOptionsView
					.setBackgroundImageButtonVisibility(true); // TODO: set to whether debug mode.
			this.setTagSelectorVisibility(false);
			this.loadModeSpecificSnapPreference();
			this.mMeasuringToggle.setVisibility(View.GONE);
            this.mDiceToggle.setVisibility(View.GONE);
            this.mDiceToggle.setChecked(false);
            this.mDice.setVisibility(View.GONE);
            this.mSelectedToolTextView.setVisibility(View.VISIBLE);
			this.mCombatView.setEditingLayerMask(this.mDrawOptionsView.isMaskToolSelected());
			return;
		case MODE_DRAW_ANNOTATIONS:
			this.mCombatView.getMultiSelect().selectNone();
			this.mCombatView.setAreTokensManipulable(false);
			this.mCombatView.useAnnotationLayer();
			this.mCombatView.setFogOfWarMode(this.mSharedPreferences
					.getBoolean("fogofwar", true) ? FogOfWarMode.CLIP
					: FogOfWarMode.NOTHING);
			this.mBottomControlFrame.removeAllViews();
			this.mBottomControlFrame.addView(this.mDrawOptionsView);
			this.setModePreference(manipulationMode);
			this.mDrawOptionsView.setDefault();
			this.mDrawOptionsView.setMaskToolVisibility(false);
			this.mDrawOptionsView.setBackgroundImageButtonVisibility(false);
            this.mDrawOptionsView.setInformationButtonVisibility(false);
			this.setTagSelectorVisibility(false);
			this.loadModeSpecificSnapPreference();
			this.mMeasuringToggle.setVisibility(View.GONE);
            this.mDiceToggle.setVisibility(View.GONE);
            this.mDiceToggle.setChecked(false);
            this.mDice.setVisibility(View.GONE);
            this.mSelectedToolTextView.setVisibility(View.VISIBLE);
			this.mCombatView.setEditingLayerMask(false);
			return;
		case MODE_DRAW_GM_NOTES:
			this.mCombatView.getMultiSelect().selectNone();
			this.mCombatView.setAreTokensManipulable(false);
			this.mCombatView.useGmNotesLayer();
			this.mCombatView.setFogOfWarMode(FogOfWarMode.NOTHING);
			this.mBottomControlFrame.removeAllViews();
			this.mBottomControlFrame.addView(this.mDrawOptionsView);
			this.setModePreference(manipulationMode);
			this.mDrawOptionsView.setDefault();
			this.mDrawOptionsView.setMaskToolVisibility(true);
			this.mDrawOptionsView.setBackgroundImageButtonVisibility(false);
            this.mDrawOptionsView.setInformationButtonVisibility(true);
			this.setTagSelectorVisibility(false);
			this.loadModeSpecificSnapPreference();
			this.mMeasuringToggle.setVisibility(View.GONE);
            this.mDiceToggle.setVisibility(View.GONE);
            this.mDiceToggle.setChecked(false);
            this.mDice.setVisibility(View.GONE);
            this.mSelectedToolTextView.setVisibility(View.VISIBLE);
			this.mCombatView.setEditingLayerMask(this.mDrawOptionsView.isMaskToolSelected());
			return;
		case MODE_TOKENS:
			this.mCombatView.useBackgroundLayer();
			this.mCombatView.setAreTokensManipulable(true);
			this.mCombatView.setTokenManipulationMode();
			this.mCombatView.setFogOfWarMode(this.mSharedPreferences
					.getBoolean("fogofwar", true) ? FogOfWarMode.CLIP
					: FogOfWarMode.NOTHING);
			this.mCombatView.setMaskAppliesToTokens(this.mSharedPreferences
					.getBoolean("mask_tokens", false));
			this.mBottomControlFrame.removeAllViews();
			this.mBottomControlFrame.addView(this.mTokenSelector);
            this.mDrawOptionsView.setInformationButtonVisibility(false);
			this.setModePreference(manipulationMode);
			this.loadModeSpecificSnapPreference();
			this.mMeasuringToggle.setVisibility(View.VISIBLE);
            this.mDiceToggle.setVisibility(View.VISIBLE);
            this.mSelectedToolTextView.setVisibility(View.GONE);
			this.mCombatView.setEditingLayerMask(false);
			return;
		default:
			throw new IllegalArgumentException("Invalid manipulation mode: "
					+ Integer.toString(manipulationMode));
		}
	}

	/**
	 * Sets the preference that will persist the active mode between sessions.
	 * 
	 * @param mode
	 *            The mode to set
	 */
	private void setModePreference(final int mode) {
		// Persist the filename that we saved to so that we can load from that
		// file again.
		Editor editor = this.mSharedPreferences.edit();
		editor.putInt("manipulation_mode", mode);
        savePrefChanges(editor);
	}

	/**
	 * Sets the snap preference associated with the current combat map mode.
	 * 
	 * @param shouldSnap
	 *            True if should snap, false otherwise.
	 */
	private void setModeSpecificSnapPreference(final boolean shouldSnap) {
		int manipulationMode = this.mSharedPreferences.getInt(
				"manipulation_mode", MODE_TOKENS);

		Editor editor = this.mSharedPreferences.edit();
		editor.putBoolean(
				this.getModeSpecificSnapPreferenceName(manipulationMode),
				this.mSnapToGridMenuItem.isChecked());
        savePrefChanges(editor);

        this.mSnapToGridMenuItem.setChecked(shouldSnap);

        if (mActionMode != null && mActionMode.getMenu().findItem(R.id.menu_snap_to_grid_context) != null) {
            mActionMode.getMenu().findItem(R.id.menu_snap_to_grid_context).setChecked(shouldSnap);
        }

		this.mCombatView.setShouldSnapToGrid(shouldSnap);
	}

	/**
	 * Sets the visibility of the tag selector.
	 * 
	 * @param visible
	 *            The new visibility.
	 */
	private void setTagSelectorVisibility(boolean visible) {
		this.mPopupFrame.setVisibility(visible ? View.VISIBLE : View.GONE);
		this.findViewById(R.id.combatMapMainLayout).requestLayout();
		this.mTagSelectorVisible = visible;
	}

	/**
	 * Queries the undo/redo state and sets the enabled state for the menu
	 * items.
	 */
	private void setUndoRedoEnabled() {
		if (this.mCombatView == null
				|| this.mCombatView.getUndoRedoTarget() == null) {
			return;
		}
		
		boolean canUndo = this.mCombatView.getUndoRedoTarget().canUndo();
		boolean canRedo = this.mCombatView.getUndoRedoTarget().canRedo();

		if (this.mUndoMenuItem != null && mUndoMenuItem.isEnabled() != canUndo) {
			this.mUndoMenuItem.setEnabled(canUndo);
			this.mUndoMenuItem
					.setIcon(this.mUndoMenuItem.isEnabled() ? mUndoMenuItemDrawable
							: R.drawable.undo_greyscale);
		}
		if (this.mRedoMenuItem != null && mRedoMenuItem.isEnabled() != canRedo) {
			this.mRedoMenuItem.setEnabled(canRedo);
			this.mRedoMenuItem
					.setIcon(this.mRedoMenuItem.isEnabled() ? mRedoMenuItemDrawable
							: R.drawable.redo_greyscale);
		}
	}

	/**
	 * Listener for actions to take when the multi-token select managed by this
	 * activity's main view changes.
	 * 
	 * @author Tim
	 * 
	 */
	private class SelectionChangedListener implements
			MultiSelectManager.SelectionChangedListener {

		@Override
		public void selectionChanged() {
			Collection<BaseToken> selected = CombatMap.this.mCombatView
					.getMultiSelect().getSelectedTokens();
			BaseToken[] selectedArr = selected.toArray(new BaseToken[selected.size()]);
			int numTokens = selected.size();
			if (CombatMap.this.mActionMode != null && selected.size() > 0) {
				Menu m = CombatMap.this.mActionMode.getMenu();
				CombatMap.this.mActionMode.setTitle(Integer.toString(numTokens)
						+ (numTokens == 1 ? " Token " : " Tokens ")
						+ "Selected.");

				// Modify the currently checked menu items based on the property
				// of the tokens selected.
				m.findItem(R.id.token_action_mode_bloodied).setChecked(
						BaseToken.allBloodied(selected));

				// Modify the currently checked border color.
				if (BaseToken.areTokenBordersSame(selected)) {
					if (selectedArr[0].hasCustomBorder()) {
						switch (selectedArr[0].getCustomBorderColor()) {
						case Color.WHITE:
							m.findItem(
									R.id.token_action_mode_border_color_white)
									.setChecked(true);
							break;
						case Color.BLUE:
							m.findItem(R.id.token_action_mode_border_color_blue)
									.setChecked(true);
							break;
						case Color.BLACK:
							m.findItem(
									R.id.token_action_mode_border_color_black)
									.setChecked(true);
							break;
						case Color.RED:
							m.findItem(R.id.token_action_mode_border_color_red)
									.setChecked(true);
							break;
						case Color.GREEN:
							m.findItem(
									R.id.token_action_mode_border_color_green)
									.setChecked(true);
							break;
						case Color.YELLOW:
							m.findItem(
									R.id.token_action_mode_border_color_yellow)
									.setChecked(true);
							break;
						default:
							break;
						}

					} else {
						m.findItem(R.id.token_action_mode_border_color_none)
								.setChecked(true);
					}
				}

				if (BaseToken.areTokenSizesSame(selected)) {
					float size = selectedArr[0].getSize();
					// CHECKSTYLE:OFF
					if (Math.abs(size - .1) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_tenth)
								.setChecked(true);
					} else if (Math.abs(size - .25) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_quarter)
								.setChecked(true);
					} else if (Math.abs(size - .5) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_half)
								.setChecked(true);
					} else if (Math.abs(size - 1) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_1).setChecked(
								true);
					} else if (Math.abs(size - 2) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_2).setChecked(
								true);
					} else if (Math.abs(size - 3) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_3).setChecked(
								true);
					} else if (Math.abs(size - 4) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_4).setChecked(
								true);
					} else if (Math.abs(size - 5) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_5).setChecked(
								true);
					} else if (Math.abs(size - 6) < Util.FP_COMPARE_ERROR) {
						m.findItem(R.id.token_action_mode_size_6).setChecked(
								true);
					}
					// CHECKSTYLE:ON
				}
			}
		}

		@Override
		public void selectionEnded() {
			if (CombatMap.this.mActionMode != null) {
				ActionMode m = CombatMap.this.mActionMode;
				CombatMap.this.mActionMode = null;
				m.finish();
			}
		}

		@Override
		public void selectionStarted() {
			CombatMap.this.mActionMode = CombatMap.this
					.startSupportActionMode(new TokenSelectionActionModeCallback());
		}
	}

	/**
	 * Task that loads the token database off the UI thread, and populates
	 * everything that needs the database when on the UI thread again.
	 * 
	 * @author Tim
	 * 
	 */
	class TokenDatabaseLoadTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			TokenDatabase.getInstance(CombatMap.this.getApplicationContext());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			CombatMap.this.mTokenDatabase = TokenDatabase
					.getInstance(CombatMap.this.getApplicationContext());
			MapData d = MapData.getInstance();
			d.getTokens().deplaceholderize(CombatMap.this.mTokenDatabase);
            mCombatView.alertTokensChanged();
			
			CombatMap.this.mTagNavigator.setShowInactiveTags(false);
            CombatMap.this.mTagNavigator.setMarkSystemTags(false);
			CombatMap.this.mTagNavigator.setTokenDatabase(CombatMap.this.mTokenDatabase);
			CombatMap.this.mTagNavigator.setTagPath(mData.getLastTag());

            CombatMap.this.mTokenSelector.setSelectedTag(TokenDatabase.ALL, null);
		}
	}

	/**
	 * Callback defining an action mode for selecting multiple tokens.
	 * 
	 * @author Tim
	 * 
	 */
	private class TokenSelectionActionModeCallback implements
			Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// Get a *list* of the selected tokens.
			List<BaseToken> tokens = new ArrayList<BaseToken>(
					CombatMap.this.mCombatView.getMultiSelect()
							.getSelectedTokens());

			if (item.getItemId() == R.id.token_action_mode_bloodied) {
				item.setChecked(!item.isChecked());
				mData.getTokens().checkpointTokens(tokens);
				for (BaseToken t : tokens) {
					t.setBloodied(item.isChecked());
				}
				mData.getTokens().createCommandHistory();
			} else if (item.getItemId() == R.id.token_action_mode_border_color_none) {
				item.setChecked(true);
				mData.getTokens().checkpointTokens(tokens);
				for (BaseToken t : tokens) {
					t.clearCustomBorderColor();
				}
				mData.getTokens().createCommandHistory();
			} else if (item.getItemId() == R.id.token_action_mode_border_color_white) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.WHITE);
			} else if (item.getItemId() == R.id.token_action_mode_border_color_blue) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.BLUE);
			} else if (item.getItemId() == R.id.token_action_mode_border_color_black) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.BLACK);
			} else if (item.getItemId() == R.id.token_action_mode_border_color_red) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.RED);
			} else if (item.getItemId() == R.id.token_action_mode_border_color_green) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.GREEN);
			} else if (item.getItemId() == R.id.token_action_mode_border_color_yellow) {
				item.setChecked(true);
				this.setTokenBorderColor(tokens, Color.YELLOW);
			} else if (item.getItemId() == R.id.token_action_mode_size_tenth) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 0.1f);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_quarter) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 0.25f);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_half) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 0.5f);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_1) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 1);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_2) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 2);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_3) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 3);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_4) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 4);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_5) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 5);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_size_6) {
				// CHECKSTYLE:OFF
				item.setChecked(true);
				this.setTokenSize(tokens, 6);
				// CHECKSTYLE:ON
			} else if (item.getItemId() == R.id.token_action_mode_delete) {
				mData.getTokens().removeAll(tokens);
				// We just deleted all the tokens, select none.
				CombatMap.this.mCombatView.getMultiSelect().selectNone();
			}
			CombatMap.this.mCombatView.alertTokensChanged();
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.token_action_mode_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			CombatMap.this.mCombatView.getMultiSelect().selectNone();
			CombatMap.this.mCombatView.refreshMap();
			// Return to token manipulation mode.
			CombatMap.this.mCombatView.setTokenManipulationMode();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		/**
		 * Sets the border color for all tokens in the given list, properly
		 * checkpointing for undo/redo.
		 * 
		 * @param tokens
		 *            The list of tokens to change.
		 * @param color
		 *            Color of the border to apply.
		 */
		private void setTokenBorderColor(List<BaseToken> tokens, int color) {
			mData.getTokens().checkpointTokens(tokens);
			for (BaseToken t : tokens) {
				t.setCustomBorder(color);
			}
			mData.getTokens().createCommandHistory();
		}

		/**
		 * Sets the size of all tokens in the given list, properly checkpointing
		 * for undo/redo.
		 * 
		 * @param tokens
		 *            The list of tokens to change.
		 * @param size
		 *            The new token size.
		 */
		private void setTokenSize(List<BaseToken> tokens, float size) {
			mData.getTokens().checkpointTokens(tokens);
			for (BaseToken t : tokens) {
				t.setSize(size);
			}
			mData.getTokens().createCommandHistory();
		}
	}

	/**
	 * Action Mode for selecting and manipulating a single image.
	 * 
	 * @author Tim
	 * 
	 */
	private class ImageSelectionActionModeCallback implements
			Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.background_image_menu, menu);
            MenuItem i = menu.findItem(R.id.background_image_maintain_aspect_ratio);
            i.setChecked(mCombatView.getSelectedBackgroundImage().shouldMaintainAspectRatio());
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			BackgroundImage selectedImage = mCombatView
					.getSelectedBackgroundImage();
			if (selectedImage == null) {
				return false;
			}

			int itemId = item.getItemId();
			if (itemId == R.id.background_image_delete) {
				mData.getBackgroundImages().deleteImage(selectedImage);
				mCombatView.setSelectedBackgroundImage(null);
				mCombatView.refreshMap();
			} else if (itemId == R.id.background_image_maintain_aspect_ratio) {
                item.setChecked(!item.isChecked());
				mData.getBackgroundImages()
						.checkpointImageBefore(selectedImage);
				selectedImage.setShouldMaintainAspectRatio(item.isChecked());
				mData.getBackgroundImages().checkpointImageAfter();
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

	}

    private class LineSelectionActionModeCallback implements
            Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.line_selection_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.line_selection_copy) {
                mCombatView.stampSelection();
            } else if (itemId == R.id.menu_undo) {
                mCombatView.getUndoRedoTarget().undo();
                mCombatView.refreshMap();
            } else if (itemId == R.id.menu_redo) {
                mCombatView.getUndoRedoTarget().redo();
                mCombatView.refreshMap();
            } else if (itemId == R.id.menu_snap_to_grid_context) {
                setModeSpecificSnapPreference(!menuItem.isChecked());
                return true;
            } else if (itemId == R.id.menu_delete_selection) {
                mCombatView.getSelection().delete();
                mCombatView.refreshMap();
                mActionMode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mCombatView.clearSelection();
        }
    }
	
	private void openDeployTokensDialog() {
		final TokenDeploymentDialog dlg = new TokenDeploymentDialog(CombatMap.this);
		final String tag = this.mTagNavigator.getCurrentTagPath();
		mPopupFrame.setVisibility(View.GONE);
		dlg.setTag(mTokenDatabase, tag);
		dlg.show();
		dlg.setOnDismissListener(new Dialog.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface d) {
				for (TokenDeploymentDialog.TokenNumberPair pair: dlg.getDeploymentList()) {
					for (int i = 0; i < pair.getCount(); ++i) {
                        try {
                            BaseToken t = pair.getToken().clone();
                            mCombatView.placeToken(t);
                            mCombatView.getMultiSelect().addToken(t);
                        } catch (CloneNotSupportedException e) {
                            Log.e(TAG, "Could not clone token for deployment", e);
                        }
					}
					mTokenDatabase.setTokenTagCount(pair.getToken().getTokenId(), tag, pair.getCount());
				}
			}
		});
	}

    private final CastManager.Callback mCastCallback = new CastManager.Callback() {

        @Override
        public void onApplicationStarted() {
            mCastViewMenuItem.setVisible(true);
        }

        @Override
        public void onApplicationStopped() {
            mCastViewMenuItem.setVisible(false);
            mStopCastingViewMenuItem.setVisible(false);

        }
    };
}
