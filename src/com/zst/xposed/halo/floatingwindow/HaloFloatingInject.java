package com.zst.xposed.halo.floatingwindow;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorBestMatch;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static de.robv.android.xposed.XposedHelpers.findField;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;


public class HaloFloatingInject implements  IXposedHookZygoteInit , IXposedHookLoadPackage{
	private static final String ID_TAG = "&ID=";
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	
	private static String class_boolean = Res.NULL;
	public static XSharedPreferences pref;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
		inject_WindowManagerService_setAppStartingWindow();
		inject_Activity();
		inject_DecorView_generateLayout();	
		//TODO : add dynamic enabler
	}
	@Override
	public void handleLoadPackage(LoadPackageParam l) throws Throwable {
		inject_ActivityRecord_ActivityRecord(l);
	}
	static Field fullscreen_3 = null;

	public static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam) {
		try {
			if (!lpparam.packageName.equals("android")) return;
			
			final Constructor<?> contructor3 = XposedHelpers.findConstructorBestMatch(
					findClass("com.android.server.am.ActivityRecord", lpparam.classLoader),
					findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader), 
					findClass("com.android.server.am.ActivityStack", lpparam.classLoader),
					findClass("com.android.server.am.ProcessRecord", lpparam.classLoader),
					int.class, Intent.class, String.class, ActivityInfo.class, Configuration.class,
		            findClass("com.android.server.am.ActivityRecord", lpparam.classLoader), 
		            String.class, int.class, boolean.class );
			
			XposedBridge.hookMethod(contructor3, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				 @Override  protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					 Intent i  = (Intent) param.args[4];
						ActivityInfo aInfo  = (ActivityInfo) param.args[6];
						 String pkg = aInfo.applicationInfo.packageName;
						 if (pkg.equals("android") || pkg.equals("com.android.systemui")) return;
						 if(aInfo.applicationInfo.uid != Res.previousUid){
							 
						 int _launchedFromUid  = (Integer) param.args[3];
						 if (_launchedFromUid == 1000) return;
						 
							 
						 if((i.getFlags()& FLAG_FLOATING_WINDOW)==0){
							 Res.notFloating = true;
							 return;
						 }
						 }
						 
						 Res.notFloating = false;
						 Res.previousUid = aInfo.applicationInfo.uid;

					   Class<?> d = findClass("com.android.server.am.ActivityRecord", lpparam.classLoader);
					   XposedBridge.log("halo:--"+d.toString() + "----" + pkg);  

					 fullscreen_3 = null;
					   for ( Field fullscreen_1 : d.getDeclaredFields() ) {
				            
				            if (fullscreen_1.getName().contains("fullscreen")){
				            fullscreen_3 = fullscreen_1;
				            fullscreen_3.setAccessible(true);
				            break;
				            }
					   }
			}
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
				   if (fullscreen_3 != null)
		            fullscreen_3.set(param.thisObject, Boolean.FALSE); 
				   
					 fullscreen_3 = null;
			   }
			 });
			
			
		
		} catch (Exception e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityRecord): " + e.toString());
		}
	}
		
	public static void inject_WindowManagerService_setAppStartingWindow() {
		try {
			findAndHookMethod("com.android.server.wm.WindowManagerService", null, "setAppStartingWindow",
					IBinder.class, String.class, int.class, CompatibilityInfo.class, CharSequence.class, 
				    int.class, int.class, int.class, IBinder.class, boolean.class, 
				    new XC_MethodHook() { 
				
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (Res.notFloating) return;
					// Gets String pkg to get package name
					String pkg = (String)param.args[1];
					if(pkg.equals("android"))return; 
					
					// Change boolean "createIfNeeded" to FALSE
					param.args[9] = Boolean.FALSE;
					// Removes Blank window placeholder before activity's layout xml fully loads
					
					XposedBridge.log("XHaloFloatingWindow-DEBUG(setAppStartingWindow):" + pkg );
					

					}
				
				});
		} catch (Throwable e) {
		}
}
	
	
	public static void inject_Activity( ) { 
		try{	
			findAndHookMethod( Activity.class,  "onCreate", Bundle.class, new XC_MethodHook() { 
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					 Activity thiz = (Activity)param.thisObject;
					 String name = thiz.getWindow().getContext().getPackageName();
					 if (name.startsWith("com.android.systemui"))  return; 
					 boolean isHoloFloat = (thiz.getIntent().getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
					 if (Res.notFloating == false){
						 isHoloFloat = true;
					 }
					 if (class_boolean.equals( name + ID_TAG + thiz.getTaskId()))
					 {   		isHoloFloat = true;
					 }else {	class_boolean = Res.NULL;
					 }
					
					 if(isHoloFloat){
						 class_boolean = name + ID_TAG + thiz.getTaskId();
						 XposedBridge.log("XHaloFloatingWindow-DEBUG(onCreate):" + class_boolean);
						 return;
					 }
					 class_boolean = Res.NULL;
					 
				}
				
			});
		
			
		} catch (Throwable e) {
		}
		
}
	
	
	public static void inject_DecorView_generateLayout() {
		try {
			findAndHookMethod("com.android.internal.policy.impl.PhoneWindow", null, "generateLayout",
					"com.android.internal.policy.impl.PhoneWindow.DecorView", new XC_MethodHook() { 
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Window window = (Window) param.thisObject;
					Context context = window.getContext();
					String AppPackage = context.getPackageName();
 
					 if (!class_boolean.startsWith(AppPackage + ID_TAG)) return; 
					 
					 XposedBridge.log("XHaloFloatingWindow-DEBUG(DecorView): " + class_boolean);
					 
						String localClassPackageName = context.getClass().getPackage().getName();

					appleFloating(context, window,localClassPackageName);
					
				}
			});
		} catch (Exception e) { XposedBridge.log("XHaloFloatingWindow-ERROR(DecorView): " + e.toString());
		}
	}
	
	public static void appleFloating(Context context , Window mWindow, String class_name ){
		try{
		Intent intent__ = new Intent(context.getPackageManager().getLaunchIntentForPackage(class_name));
	        	ResolveInfo rInfo = context.getPackageManager().resolveActivity(intent__, 0);
	        	ActivityInfo info = rInfo.activityInfo;	            
	        	TypedArray ta = context.obtainStyledAttributes(info.theme, com.android.internal.R.styleable.Window);
	        	
	            TypedValue backgroundValue = ta.peekValue(com.android.internal.R.styleable.Window_windowBackground);
	            // Apps that have no title don't need no title bar
	            boolean gotTitle = ta.getBoolean(com.android.internal.R.styleable.Window_windowNoTitle, false);
	            if (gotTitle) mWindow.requestFeature(Window.FEATURE_NO_TITLE);
	           
	            if (backgroundValue != null && backgroundValue.toString().contains("light")) {
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindowLight, true);
	            } else {  //Checks if light or dark theme
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
	            }
	            
	            ta.recycle();
		}catch(Throwable t){
            context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
		}
	            // Create our new window
	           //mWindow.mIsFloatingWindow = true; < We dont need this. onCreate Hook will compare getTaskId and resize accordingly
	            mWindow.setCloseOnTouchOutsideIfNotSet(true);
	            mWindow.setGravity(Gravity.CENTER);
	            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	            WindowManager.LayoutParams params = mWindow.getAttributes(); 
	            //TODO : transparency amount customization
				Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
				Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

	            params.alpha = alp;	
	            params.dimAmount = dimm;
	            mWindow.setAttributes((android.view.WindowManager.LayoutParams) params);
		        scaleFloatingWindow(context,mWindow);
	}

	public static void scaleFloatingWindow(Context context ,  Window mWindow ) {		
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics); 
        //TODO : make width/height customizable 
        if (metrics.heightPixels > metrics.widthPixels) { // portrait 
            mWindow.setLayout((int)(metrics.widthPixels * 0.95f), (int)(metrics.heightPixels * 0.7f));
        } else {  // landscape
        	mWindow.setLayout((int)(metrics.widthPixels * 0.7f), (int)(metrics.heightPixels * 0.85f));
        }
    }

}


 
