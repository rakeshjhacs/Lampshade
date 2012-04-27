package com.syn3rgy.lampshade;

import java.io.IOException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Toast;

import com.syn3rgy.tropeswrapper.TropesArticle;
import com.syn3rgy.tropeswrapper.TropesHelper;

/** Shows a single TvTropes article */
public class ArticleActivity extends Activity {
	TropesApplication application;
	Uri url;
	ActionMode mActionMode = null;
	// Used to pass the selected link to the ActionBar
	String selectedLink = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_activity);
		
		ActionBar ab = getActionBar();
		ab.setHomeButtonEnabled(true);
		
		this.application = (TropesApplication) getApplication();
		
		WebView wv = (WebView) findViewById(R.id.wv_content);
		wv.setOnTouchListener(new OnTouchListener() {
			
			// Disable the ActionMode when the WebView is touched
			public boolean onTouch(View v, MotionEvent event) {
				if(mActionMode != null) {
					mActionMode.finish();
					return true;
				}
				else {
					return false;
				}
			}
			
		});
		
		Uri data = getIntent().getData();
		if(data != null) {
			this.url = data;
			new loadArticleTask(this).execute(data);
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.article_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
        	openActivity(MainActivity.class);
        	return true;
        case R.id.refresh_article:
        	loadPage(url.toString());
        	return true;
        default:
        	return super.onOptionsItemSelected(item);
        }
    }
    
    private void loadPage(String url) {
    	Intent pageIntent = new Intent(getApplicationContext(), ArticleActivity.class);
    	pageIntent.setData(Uri.parse(url));
    	startActivity(pageIntent);
    }
    
    private void openActivity(Class cls) {
    	Intent intent = new Intent(getApplicationContext(), cls);
    	startActivity(intent);
    }
    
    private void saveArticle(String url) {
    	new saveArticleTask().execute(url);
    }
	
    /** Does all preparations and initialises the ActionMode */
    private boolean startLinkMode(String url) {
    	selectedLink = url;
    	
    	if (mActionMode != null) {
            return false;
        }

        mActionMode = this.startActionMode(mActionModeCallback);
        return true;
    }
    
    /** Loads an article in a different thread */
	public class loadArticleTask extends AsyncTask<Uri, Integer, TropesArticle> {
		public loadArticleTask(Activity activity) {
			this.activity = activity;  
		}
		
		private ProgressDialog pDialog = null;
		private Activity activity;
		
		@Override
		protected void onPreExecute() {
			this.pDialog = ProgressDialog.show(this.activity, "", "Loading article...", true);
			pDialog.show();
		}
		
		@Override
		protected TropesArticle doInBackground(Uri... params) {
			TropesArticle article = null;
			try {
				// params[0] is the URL
				article = new TropesArticle(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return article;
		}
		
		@Override
		protected void onPostExecute(TropesArticle article) {
			if(pDialog.isShowing()) {
				pDialog.dismiss();
			}
			if(article != null) {
				activity.getActionBar().setTitle(article.title);
				WebView wv = (WebView) findViewById(R.id.wv_content);
				wv.loadData(article.content, "text/html", null);
				
				wv.setOnLongClickListener(new OnLongClickListener() {
					public boolean onLongClick(View v) {
						WebView wv = (WebView) v;
						HitTestResult hr = wv.getHitTestResult();
						
						// If the clicked element is a link
						if(hr.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
							// hr.getExtra() is the link's target
							startLinkMode(hr.getExtra());
						}
						return true;
					}
				});
			}
		}
	}
	
	// It is not strictly necessary to do this in a separate thread, but no need to (possibly) block the UI with the database
	public class saveArticleTask extends AsyncTask<String, Integer, ArticleItem> {

		@Override
		protected ArticleItem doInBackground(String... params) {
			Uri url = Uri.parse(params[0]);
			
			application.articlesSource.open();
			ArticleItem item = application.articlesSource.createArticleItem(TropesHelper.titleFromUrl(url), url);
			application.articlesSource.close();
			return item;
		}
		
		@Override protected void onPostExecute(ArticleItem item) {
			Toast.makeText(getApplicationContext(), "Added " + item.title, Toast.LENGTH_SHORT).show();
		}
	}
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			selectedLink = null;
		}
		
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.article_action_menu, menu);
			if(selectedLink != null) {
				mode.setTitle(TropesHelper.titleFromUrl(Uri.parse(selectedLink)));
			}
			return true;
		}
		
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()) {
			case R.id.article_action_save:
				if(selectedLink != null) {
					saveArticle(selectedLink);
					mode.finish();
					return true;
				}
				else {
					return false;
				}
			default:
				return false;
			}
		}
	};
}
