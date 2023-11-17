import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-file-delete' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const FileDelete = NativeModules.FileDelete
  ? NativeModules.FileDelete
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type Params = {
  byNames: boolean;
  files: Array<string>;
};

export function deleteFile(options: Params): Promise<any> {
  return FileDelete.deleteFile(options);
}

export function getContentUrisByNames(files: Array<string>): Promise<any> {
  return FileDelete.getContentUrisByNames(files);
}

export function getContentUrisByFileSystemUris(files: Array<string>): Promise<any> {
  return FileDelete.getContentUrisByFileSystemUris(files);
}
