package eu.prismsw.lampshade.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import eu.prismsw.lampshade.R;
import eu.prismsw.lampshade.listeners.OnInteractionListener;
import eu.prismsw.lampshade.listeners.OnLoadListener;
import eu.prismsw.lampshade.tasks.LoadTropesTask;
import eu.prismsw.tools.android.UIFunctions;
import eu.prismsw.tropeswrapper.TropesArticle;
import eu.prismsw.tropeswrapper.TropesArticleInfo;
import eu.prismsw.tropeswrapper.TropesArticleSettings;

public class ArticleFragment extends TropesFragment {
	
	public static ArticleFragment newInstance(Uri url) {
		ArticleFragment f = new ArticleFragment();
		Bundle bundle = new Bundle(2);
		bundle.putParcelable(PASSED_URL, url);
		bundle.putParcelable(TRUE_URL, url);
		f.setArguments(bundle);
		return f;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
		return inflater.inflate(R.layout.article_fragment, group, false);
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.article_fragment_menu, menu);
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.article_find) {
			WebView wv = (WebView) getView().findViewById(R.id.wv_content);
			wv.showFindDialog("", true);
			return true;
		}
        else if(item.getItemId() == R.id.article_show_spoilers) {
        	showAllSpoilers();
        	return true;
        }
        else {
			return super.onOptionsItemSelected(item);
		}
    }
    
    private void showAllSpoilers() {
        	WebView wv = (WebView) getView().findViewById(R.id.wv_content);
        	
        	String showSpoilers = "function() { var spoilers = document.getElementsByClassName('spoiler'); for(i = 0; i < spoilers.length; i++) { showSpoiler(spoilers[i]); } }";
        	wv.loadUrl("javascript:(" + showSpoilers + ")()");
    }
	
    /** Loads an article in a different thread */
	public class LoadArticleTask extends LoadTropesTask {
		
		public LoadArticleTask(OnLoadListener tLoadListener, OnInteractionListener tInteractionListener) {
			super(tLoadListener, tInteractionListener);
		}
		
		public LoadArticleTask(OnLoadListener tLoadListener, OnInteractionListener tInteractionListener, TropesArticleSettings articleSettings) {
			super(tLoadListener, tInteractionListener, articleSettings);
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if(result.getClass() == TropesArticle.class) {
				TropesArticle article = (TropesArticle) result;
				
				WebView wv = (WebView) getView().findViewById(R.id.wv_content);
				wv.getSettings().setJavaScriptEnabled(true);
				wv.getSettings().setLoadsImagesAutomatically(true);
				wv.loadDataWithBaseURL("tvtropes.org", article.content.html(), "text/html", "utf-8", null);
				
				wv.setWebChromeClient(new WebChromeClient() {
				    @Override  
				    public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
				    {  
				    	UIFunctions.showToast(message, application);
				    	return true;
				    };  
				});
				
				wv.setOnLongClickListener(new OnLongClickListener() {
					public boolean onLongClick(View v) {
						WebView wv = (WebView) v;
						HitTestResult hr = wv.getHitTestResult();
						
						// If the clicked element is a link
						if(hr.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
							// hr.getExtra() is the link's target
							tInteractionListener.onLinkSelected(Uri.parse(hr.getExtra()));
						}
						return true;
					}
				});
				
				wv.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						tInteractionListener.onLinkClicked(Uri.parse(url));
						return true;
					}
				});
				
				TropesArticleInfo tArticleInfo = new TropesArticleInfo(article.title, article.url, article.subpages);
				trueUrl = article.url;
				articleInfo = tArticleInfo;
				tLoadListener.onLoadFinish(tArticleInfo);
			}
			else {
				Exception e = (Exception) result;
				this.tLoadListener.onLoadError(e);
			}
		}
	}
	
	public void loadTropes(Uri url) {
		String theme = application.getThemeName();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
		Integer fontSize = preferences.getInt("preference_font_size", 12);
		String fontSizeStr = fontSize.toString() + "pt";
		
		TropesArticleSettings articleSettings;
		if(theme.equalsIgnoreCase("HoloDark")) {
			articleSettings = new TropesArticleSettings(true);
		}
		else {
			articleSettings = new TropesArticleSettings(false);
		}
		articleSettings.fontSize = fontSizeStr;
		
		
		new LoadArticleTask(this.loadListener, this.interactionListener, articleSettings).execute(url);
	}
}
