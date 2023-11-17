# React Native FileDelete

A file-delete for deleting all file types in the ExternalStorage(Download, Images, Videos, Musique, etc..) of your Android device from react-native.
Ex: html, pdf, jpeg, mp3, mp4, docx, etc..

## Installation

```sh
npm install react-native-file-delete
```
Or
```sh
yarn add react-native-file-delete
```

### Option 1: Automatic

```sh
react-native link react-native-file-delete
```

### Option 2: Manual 
#### Android
* Edit android/settings.gradle to included
```
include ':react-native-file-delete'
project(':react-native-file-delete').projectDir = new File(rootProject.projectDir,'../node_modules/react-native-file-delete/android')
```

* Edit android/app/build.gradle file to include
```
dependencies {
  ....
  compile project(':react-native-file-delete')
}
```

* Edit MainApplication.java to include
```
// import the package
import com.filedelete.FileDeletePackage;
```

For Android 10 support you have to place the following line in your AndroidManifest.xml tag
android:requestLegacyExternalStorage="true"
```
<application
      android:name=".MainApplication"
      android:label="@string/app_name"
      android:icon="@mipmap/logo"
      android:roundIcon="@mipmap/logo"
      android:allowBackup="false"
      android:theme="@style/AppTheme"
      // ...
      android:requestLegacyExternalStorage="true"> 
// ...
</application>
```

## Usage

FileDelete module provide 03 functions :
* deleteFile
  For delete a files by filenames or fileSystemUris so you can only use one parameter type.
  There are 03 ways to initialize your options like shown below.

    ```js
    import { deleteFile } from 'react-native-file-delete';

    // ...

    // 1- By Default byNames is false, options contain only External File Uri
    let options = { 
        files: [ 'storage/emulated/0/Download/20180702_121938.pdf', 'storage/emulated/0/Download/10180702_121938.pdf' ]
    };
    
    // 2- byNames is false here, options contain External File Uri and byNames
    let options = { 
        files: [ 'storage/emulated/0/Download/20180702_121938.pdf', 'storage/emulated/0/Download/10180702_121938.pdf' ], 
        byNames: false 
    };

    // 3- byNames is true, options contain filenames and byNames
    let options = { 
        files: [ '20180702_121938.pdf', '10180702_121938.pdf' ], 
        byNames: true 
    };

    const deletedFileUris = await deleteFile(options);
    ```
* getContentUrisByNames
    To get the file uri content:// by filename 
    Ex: From 20180702_121938.pdf 
        To content://media/external/file/1000060588

    ```js
    import { getContentUrisByNames } from 'react-native-file-delete';

    // ...
    let options = [ '20180702_121938.pdf', '10180702_121938.pdf' ];
    const uris = await getContentUrisByNames(options); 
    ```
* getContentUrisByFileSystemUris
    To get the file uri by external file system uri so function return uri from file://  to content:// 
    Ex: From file://storage/emulated/0/Download/20180702_121938.pdf 
        To content://media/external/file/1000060588

    ```js
    import { getContentUrisByFileSystemUris } from 'react-native-file-delete';
    
    // ...
    let options = [ 'storage/emulated/0/Download/20180702_121938.pdf', 'storage/emulated/0/Download/10180702_121938.pdf' ];
    const uris = await getContentUrisByFileSystemUris(options);
    ```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
