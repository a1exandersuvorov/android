package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class OfflineFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaOfflineListAdapter adapterList;
	MegaOfflineGridAdapter adapterGrid;
	OfflineFragment offlineFragment = this;
	DatabaseHandler dbH = null;
	ArrayList<MegaOffline> mOffList= null;
	
	long parentHandle = -1;
	boolean isList = true;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	//ArrayList<String> paths = null;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<String> documents = getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_trash:{
					
					for (int i=0;i<documents.size();i++){
						File f = new File(documents.get(i));
						try{
							Util.deleteFolderAndSubfolders(context, f.getParentFile());
						}
						catch(Exception e){};
					}
					
					clearSelections();
					hideMultipleSelect();
					refreshPaths();
					break;
				}	
				
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapterList.setMultipleSelect(false);
			listView.setOnItemLongClickListener(offlineFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<String> selected = getSelectedDocuments();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			
			if (selected.size() > 0) {
				showTrash = true;
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			
			return false;
		}
		
	}
	
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		dbH = new DatabaseHandler(context);
		
		mOffList = new ArrayList<MegaOffline>();		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(getString(R.string.section_saved_for_offline));	
		((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
		((ManagerActivity)context).supportInvalidateOptionsMenu();
		
		if (isList){
			View v = inflater.inflate(R.layout.fragment_offlinelist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.offline_view_browser);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text);		
						
			mOffList=dbH.findByPath("/");
			log("He encontrado: "+mOffList.size());			
				
			for(int i=0; i<mOffList.size();i++){
				
				File offlineDirectory = null;
				if (Environment.getExternalStorageDirectory() != null){
					offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
				}
				else{
					offlineDirectory = context.getFilesDir();
				}	
				
				if (!offlineDirectory.exists()){

					log("No Encuentro el fichero");				
					
					dbH.removeById(mOffList.get(i).getId());
					log("quitado DB");
					mOffList.remove(i);
					
				}			
			}
			
			if (adapterList == null){
				adapterList = new MegaOfflineListAdapter(this, context, mOffList, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterList.setNodes(mOffList);
			}
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
			
//			setPaths(paths);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_offlinegrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.offline_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.offline_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_grid_empty_text);			
			
			File offlineDirectory = null;
			if (Environment.getExternalStorageDirectory() != null){
				offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
			}
			else{
				offlineDirectory = context.getFilesDir();
			}
			
			if (offlineDirectory.exists() && offlineDirectory.isDirectory()){
				File[] fList = offlineDirectory.listFiles();
				for (File f : fList){
					String [] s = f.getName().split("_");
					if (s.length > 0){
						long handle = -1;
						try{
							handle = Long.parseLong(s[0]);
						}
						catch(Exception e){ }
						
						if (handle != -1){
							//paths.add(f.getAbsolutePath());		
						}						
					}					
				}
			}
			else{
				offlineDirectory.mkdirs();
			}
			
//			if (adapterGrid == null){
//				adapterGrid = new MegaOfflineGridAdapter(this, context, paths, listView, emptyImageView, emptyTextView, aB);
//			}
//			else{
//				adapterGrid.setPaths(paths);
//			}
//			adapterGrid.setPositionClicked(-1);
//			
//			listView.setAdapter(adapterGrid);
			
//			setPaths(paths);
			
			return v;
		}		
	}
		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		log("onItemClick");
		if (isList){
			log("mode List");
			if (adapterList.isMultipleSelect()){
				log("multiselect");
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				if (checkedItems.get(position, false) == true){
					listView.setItemChecked(position, true);
				}
				else{
					listView.setItemChecked(position, false);
				}				
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
				
				MegaOffline currentNode = mOffList.get(position);		
				File currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
				
				if(currentFile.exists()&&currentFile.isDirectory()){

					mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
					log("He encontrado: "+mOffList.size());


					for(int i=0; i<mOffList.size();i++){

						File offlineDirectory = null;
						if (Environment.getExternalStorageDirectory() != null){
							offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
						}
						else{
							offlineDirectory = context.getFilesDir();
						}	

						if (!offlineDirectory.exists()){
							//Updating the DB because the file does not exist
							
							log("No Encuentro el fichero");				
							
							dbH.removeById(mOffList.get(i).getId());
							log("quitado DB");
							mOffList.remove(i);
						}			
					}

					if (adapterList == null){
						adapterList = new MegaOfflineListAdapter(this, context, mOffList, listView, emptyImageView, emptyTextView, aB);
					}
					else{
						adapterList.setNodes(mOffList);
					}

					adapterList.setPositionClicked(-1);
					adapterList.setMultipleSelect(false);
					notifyDataSetChanged();

				}else{
					if(currentFile.exists()&&currentFile.isFile()){
						//Open it!
						log("is FILE");
						if (MimeType.typeForName(currentFile.getName()).isImage()){
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", position);
							intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
							intent.putExtra("parentNodeHandle", -1L);
							intent.putExtra("offlinePathDirectory", currentFile.getParent());
							startActivity(intent);
						}
						else{
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
							if (ManagerActivity.isIntentAvailable(context, viewIntent)){
								context.startActivity(viewIntent);
							}
							else{
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
								if (ManagerActivity.isIntentAvailable(context, intentShare)){
									context.startActivity(intentShare);
								}
							}
						}
						
					}
					
				}
				

				
				
				
//				String currentPath = paths.get(position);
//				File currentFile = new File (currentPath);
				
//				if (MimeType.typeForName(currentFile.getName()).isImage()){
//					Intent intent = new Intent(context, FullScreenImageViewer.class);
//					intent.putExtra("position", position);
//					intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
//					intent.putExtra("parentNodeHandle", -1L);
//					startActivity(intent);
//				}
//				else{
//					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//					if (ManagerActivity.isIntentAvailable(context, viewIntent)){
//						context.startActivity(viewIntent);
//					}
//					else{
//						Intent intentShare = new Intent(Intent.ACTION_SEND);
//						intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//						if (ManagerActivity.isIntentAvailable(context, intentShare)){
//							context.startActivity(intentShare);
//						}
//					}
//				}
			}
		}
    }
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.getPositionClicked() == -1){
			clearSelections();
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapterList.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<String> documents = getSelectedDocuments();
		int files = 0;
		int folders = 0;
		for (String document : documents) {
			File f = new File(document);
			if (f.isFile()) {
				files++;
			} 
			else {
				folders++;
			}
		}
		Resources res = getActivity().getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = "";
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
		// actionMode.
	}
	
	/*
	 * Get list of all selected documents
	 */
	private List<String> getSelectedDocuments() {
		ArrayList<String> documents = new ArrayList<String>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				String currentPath = adapterList.getPathAt(checkedItems.keyAt(i));
				if (currentPath != null){
					documents.add(currentPath);
				}
			}
		}
		return documents;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){

		if (isList){
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
//			else if{
				//TODO En caso de que no esté en el raíz del offline, pues navegar para atrás.
				// Esto es, poner el nuevo path y adapterList.setNodes() y adapterList.notifyDataSetChanged();
//				return 2;
//			}
			else{
					return 0;
			}
		}
	
		return 0;
//		else{
//			parentHandle = adapterGrid.getParentHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			
//			if (adapterGrid.getPositionClicked() != -1){
//				adapterGrid.setPositionClicked(-1);
//				adapterGrid.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
//				if (parentNode != null){
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//						aB.setTitle(getString(R.string.section_cloud_drive));	
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
//					adapterGrid.setNodes(nodes);
//					listView.setSelection(0);
//					adapterGrid.setParentHandle(parentHandle);
//					return 2;
//				}
//				else{
//					return 0;
//				}
//			}
//		}
	}
	
	public ListView getListView(){
		return listView;
	}
	
	public void setNodes(ArrayList<MegaOffline> mOff){
		
		this.mOffList = mOff;
		

		if (isList){
			if (adapterList != null){
				adapterList.setNodes(mOffList);
				if (adapterList.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
		}
//		else{
//			if (adapterGrid != null){
//				adapterGrid.setPaths(paths);
//				if (adapterGrid.getCount() == 0){
//					listView.setVisibility(View.GONE);
//					emptyImageView.setVisibility(View.VISIBLE);
//					emptyTextView.setVisibility(View.VISIBLE);
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextView.setText(R.string.file_browser_empty_folder);
//				}
//				else{
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//				}			
//			}
//		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setPositionClicked(positionClicked);
			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}
	
	public void refreshPaths(){
		
		mOffList=dbH.findByPath("/");	
		
		
		setNodes(mOffList);
		listView.invalidateViews();
	}
	
	public void setPath(String path){
		
		mOffList=dbH.findByPath("/");	
		
		
		setNodes(mOffList);
		listView.invalidateViews();
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("OfflineFragment", log);
	}
}
