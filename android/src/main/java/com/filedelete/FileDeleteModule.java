package com.filedelete;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.lang.Long;
import java.lang.Exception;


import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.modules.core.PermissionAwareActivity;

import java.net.URI;
import java.util.ArrayList;
import java.lang.SecurityException;

import android.content.ContentUris;
import android.util.Log;
import android.content.Intent;
import android.app.Activity;
import android.os.Environment;


import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.Manifest;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.Settings;

import com.facebook.react.ReactActivity;

@ReactModule(name = FileDeleteModule.NAME)
public class FileDeleteModule extends ReactContextBaseJavaModule implements PermissionListener {
  public static final String NAME = "FileDelete";

  private static final String ERROR_ACTIVITY_NULL = "ERROR_ACTIVITY_NULL";
  private static final String ERROR_FILE_LIST = "ERROR_FILE_LIST";
  private static final String ERROR_URIS_PARAMETER_NULL = "ERROR_URIS_PARAMETER_NULL";
  private static final String ERROR_NOT_BOOLEAN = "ERROR_NOT_BOOLEAN";
  private static final String ERROR_NOT_ARRAY = "ERROR_NOT_ARRAY";
  private static final String ERROR_INTENT_SENDER_EXCEPTION = "ERROR_INTENT_SENDER_EXCEPTION";
  private static final String ERROR_FILENAMES_PARAMETER_NULL = "ERROR_FILENAMES_PARAMETER_NULL";
  private static final String ERROR_PERMISSION_REJECT = "ERROR_PERMISSION_REJECT";
  private static final String ERROR_FUNCTION_NOT_EXIST = "ERROR_FUNCTION_NOT_EXIST";
 
  private static final int STORAGE_PERMISSION_CODE = 23;
  private static final int INTENT_SENDER_REQUEST = 100;

  private Context context;
  private Uri queryUri;
  private Promise globalPromise;
  private String functionName;
  private ReadableArray files;
  private ReadableMap globalReadableMap;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if(resultCode == Activity.RESULT_OK) { 
                 if(globalPromise != null) {
                      switchCaseFunctionHandler();
                  }
            }
            if(resultCode == Activity.RESULT_CANCELED) { 
                if(globalPromise != null) {
                    if(checkStoragePermissions()) {
                        switchCaseFunctionHandler();
                    } else {
                        Toast.makeText(activity, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                        globalPromise.reject(ERROR_PERMISSION_REJECT, "Error: Permission Denied !");
                    }
                }
            }
        }
    }

  };


  public FileDeleteModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context = reactContext.getApplicationContext(); 
    this.queryUri = MediaStore.Files.getContentUri("external");
    reactContext.addActivityEventListener(mActivityEventListener);
  }


  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void deleteFile(ReadableMap readableMap, Promise promise) {
      // Check permission before
      if(!checkStoragePermissions()) {
          this.globalPromise = promise;
          this.functionName = "deleteFile";
          this.globalReadableMap = readableMap;
          requestForStoragePermissions();
      } else {
          fileDeletion(readableMap, promise);
      }
  }

  @ReactMethod 
  public void getContentUrisByNames(ReadableArray filenames, Promise promise) {
      if(filenames == null) {
          promise.reject(ERROR_FILENAMES_PARAMETER_NULL, "Error: Filename(s) can't be null");
          return;
      }

      // Check permission before
      if(!checkStoragePermissions()) {
          this.globalPromise = promise;
          this.functionName = "getContentUrisByNames";
          this.files = filenames;
          requestForStoragePermissions();
      } else {
            ArrayList<Uri> arrayList = getContentUrisByNames(filenames);
            promise.resolve(this.listToArrayOfString(arrayList));
      }
  }

  @ReactMethod 
  public void getContentUrisByFileSystemUris(ReadableArray fileSystemUris, Promise promise) {
      if(fileSystemUris == null) {
          promise.reject(ERROR_URIS_PARAMETER_NULL, "Error: Uri(s) can't be null");
          return;
      }

      // Check permission before
      if(!checkStoragePermissions()) {
          this.globalPromise = promise;
          this.functionName = "getContentUrisByFileSystemUris";
          this.files = fileSystemUris;
          requestForStoragePermissions();
      } else {
          ArrayList<Uri> arrayList = getContentUrisByFileSystemUris(fileSystemUris);
          promise.resolve(this.listToArrayOfString(arrayList));
      }
  }

  /**
   * Simple function to execute for file deletion
   * @param readableMap
   * @param promise
   */
  public void fileDeletion(ReadableMap readableMap, Promise promise) {
    Activity activity = getCurrentActivity();
    if(activity == null) {
        promise.reject(ERROR_ACTIVITY_NULL, "Error: Activity can't be null");
        return;
    }

    ArrayList<Uri> uris = null;
    if(readableMap.hasKey("byNames")) {
        if(ReadableType.Boolean != readableMap.getType("byNames")) {
            promise.reject(ERROR_NOT_BOOLEAN, "Error: This field have to be a Boolean");
            return;
        }
        if(!readableMap.hasKey("files")) {
            promise.reject(ERROR_FILE_LIST, "Error: files to delete can't be null");
            return;
        }
        if(ReadableType.Array != readableMap.getType("files")) {
            promise.reject(ERROR_NOT_ARRAY, "Error: This field have to be an Array");
            return;
        }

        boolean byNames = readableMap.getBoolean("byNames");
        if(byNames) {
            ReadableArray filenames = (ReadableArray)readableMap.getArray("files");
            uris = getContentUrisByNames(filenames);
        } else {
            ReadableArray fileSystemUris = (ReadableArray)readableMap.getArray("files");
            uris = getContentUrisByFileSystemUris(fileSystemUris);
        }
    } else {
        if(!readableMap.hasKey("files")) {
            promise.reject(ERROR_FILE_LIST, "Error: files to delete can't be null");
            return;
        }
        if(ReadableType.Array != readableMap.getType("files")) {
            promise.reject(ERROR_NOT_ARRAY, "Error: This field have to be an Array");
            return;
        }

        ReadableArray fileSystemUris = (ReadableArray)readableMap.getArray("files");
        uris = getContentUrisByFileSystemUris(fileSystemUris);
    }
    
    if(uris == null) {
        promise.reject(ERROR_URIS_PARAMETER_NULL, "Error: uri(s) can't be null");
        return;
    }
    
    ContentResolver contentResolver = this.context.getContentResolver();
    try {
        for(int i = 0; i < uris.size(); i++) {
            // delete object using resolver
            Uri uri = uris.get(i);
            contentResolver.delete(uri, null, null);
        }
    } catch (SecurityException e) {

        PendingIntent pendingIntent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //if exception is recoverable then again send delete request using intent
            if (e instanceof RecoverableSecurityException) {
                RecoverableSecurityException exception = (RecoverableSecurityException) e;
                pendingIntent = exception.getUserAction().getActionIntent();
            }
        }

        if (pendingIntent != null) {
            IntentSender sender = pendingIntent.getIntentSender();
            try {
                activity.startIntentSenderForResult(
                    sender,
                    INTENT_SENDER_REQUEST,
                    null, 0, 0, 0
                );
            } catch (IntentSender.SendIntentException ex) {
                promise.reject(ERROR_INTENT_SENDER_EXCEPTION, ex.getMessage());
            }
        }
    }
    
    promise.resolve(this.listToArrayOfString(uris));
  }

  /**
   * get uri of type content:// from filename
   * @param filenames
   * @return ArrayList<Uri>
   */
  public ArrayList<Uri> getContentUrisByNames(ReadableArray filenames) {
    String[] projection = new String[]{ MediaStore.Files.FileColumns._ID };
    String[] selectionArgs = new String[filenames.size()];
    int j = 0;
    for(int i = 0; i < filenames.size(); i++) {
        if(filenames.getType(i) == ReadableType.String) { 
          if(filenames.getString(i) != null || !filenames.getString(i).isEmpty()) {
            selectionArgs[j] = filenames.getString(i);
            j++;
          }
        }
    }

    String innerWhere = "?";
    for(int i = 1; i < selectionArgs.length; i++) {
        innerWhere += ", ?";
    }

    String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " IN (" + innerWhere + ")";
    String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

    Cursor cursor = this.context.getContentResolver().query(this.queryUri, projection,  selection, selectionArgs, sortOrder);

    ArrayList<Uri> arrayList = new ArrayList();
    while(cursor.moveToNext()) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
        Uri deleteUri = ContentUris.withAppendedId(this.queryUri, id);
        arrayList.add(deleteUri);
    }
    
    return arrayList;
  }

  /**
   * get uri of type content:// from uri of type file://
   * @param fileSystemUris
   * @return ArrayList<Uri>
   */
  public ArrayList<Uri> getContentUrisByFileSystemUris(ReadableArray fileSystemUris) {
      String[] projection = new String[]{ MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA };
      String[] selectionArgs = new String[fileSystemUris.size()];
      int j = 0;
      for(int i = 0; i < fileSystemUris.size(); i++) {
          if(fileSystemUris.getType(i) == ReadableType.String) {
            if(fileSystemUris.getString(i) != null || !fileSystemUris.getString(i).isEmpty()) {
              Uri uri = Uri.parse(fileSystemUris.getString(i));
              selectionArgs[j] = uri.getPath();
              j++;
            }
          }
      }

      String innerWhere = "?";
      for(int i = 1; i < selectionArgs.length; i++) {
          innerWhere += ", ?";
      }

      String selection = MediaStore.Files.FileColumns.DATA + " IN (" + innerWhere + ")";
      String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

      Cursor cursor = this.context.getContentResolver().query(this.queryUri, projection,  selection, selectionArgs, sortOrder);
      
      ArrayList<Uri> arrayList = new ArrayList();
      while(cursor.moveToNext()) {
          long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
          String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
          Uri deleteUri = ContentUris.withAppendedId(this.queryUri, id);
          arrayList.add(deleteUri);
      }
    
      return arrayList;
  }

  /**
   * Check if user has permission to access all files
   * @return boolean
   */
  public boolean checkStoragePermissions() {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          //Android is 11 (R) or above
          return Environment.isExternalStorageManager();
      } else {
          //Below android 11
          int write = ContextCompat.checkSelfPermission(this.context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
          int read = ContextCompat.checkSelfPermission(this.context, Manifest.permission.READ_EXTERNAL_STORAGE);
          
          return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
      }
  }

  /**
   * Request Permission to manage all app files
   */
  private void requestForStoragePermissions() {
      //Android is 11 (R) or above
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Activity activity = getCurrentActivity(); 
          Intent intent = null;
            
          try {
              intent = new Intent();
              intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
              intent.addCategory("android.intent.category.DEFAULT");
              Uri uri = Uri.fromParts("package", activity.getPackageName(), null); 
              intent.setData(uri);
          }catch (Exception e){
              intent = new Intent();
              intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
          } finally {
              if(intent != null) {
                  activity.startActivityForResult(intent, STORAGE_PERMISSION_CODE);
              }
          }
      
      } else {
          //Below android 11
          PermissionAwareActivity activity = (PermissionAwareActivity)getCurrentActivity();
          activity.requestPermissions(
                  new String[] {
                          Manifest.permission.WRITE_EXTERNAL_STORAGE,
                          Manifest.permission.READ_EXTERNAL_STORAGE
                  },
                  STORAGE_PERMISSION_CODE,
                  this
          );
      }

  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      if(requestCode == STORAGE_PERMISSION_CODE) {
          Activity activity = getCurrentActivity();
          if(grantResults.length > 0) {
              boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
              boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

              if(read && write) {
                   Toast.makeText(activity, "Storage Permissions Granted", Toast.LENGTH_SHORT).show();
                   if(globalPromise != null) {
                        switchCaseFunctionHandler();
                   }
              } else {
                  Toast.makeText(activity, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                  if(globalPromise != null) {
                      globalPromise.reject(ERROR_PERMISSION_REJECT, "Error: Permission Denied !");
                  }
              }
          }
      }

      return true;
  }

  /**
   * Response handle depending of function called
   */
  public void switchCaseFunctionHandler() {
      ArrayList<Uri> arrayList = null;
      switch (functionName) {
          case "deleteFile":
              if(globalReadableMap != null) {
                  fileDeletion(globalReadableMap, globalPromise);
              }
              break;
          case "getContentUrisByNames":
              if(files != null) {
                  arrayList = getContentUrisByNames(files);
                  globalPromise.resolve(listToArrayOfString(arrayList));
              }
              break;
          case "getContentUrisByFileSystemUris":
              if(files != null) {
                  arrayList = getContentUrisByFileSystemUris(files);
                  globalPromise.resolve(listToArrayOfString(arrayList));
              }
              break;
          default:
              globalPromise.reject(ERROR_FUNCTION_NOT_EXIST, "Error: No function found !");
              break;
      }
  }

  /**
   * Convert an arraylist of uris to react native WritableArray
   * @param uris
   * @return WritableArray
   */
  public WritableArray listToArrayOfString(ArrayList<Uri> uris) {
      WritableArray promiseArray = Arguments.createArray();
      for(Uri uri : uris) {
          promiseArray.pushString(uri.toString());
      }
      return promiseArray;
  }

 
}
