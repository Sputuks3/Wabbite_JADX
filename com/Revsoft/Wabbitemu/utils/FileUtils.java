package com.Revsoft.Wabbitemu.utils;

import android.os.AsyncTask;
import android.os.FileObserver;
import com.google.ads.AdSize;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public class FileUtils {
    private Set<String> mFiles = new HashSet();
    private final List<FileObserver> mObservers = new ArrayList();
    private CountDownLatch mSearchLatch;

    private class SingleFileObserver extends FileObserver {
        private final String mPath;

        public SingleFileObserver(String path, int mask) {
            super(path, mask);
            this.mPath = path;
        }

        public void onEvent(int event, String path) {
            FileUtils.this.handleFileEvent(event, this.mPath + "/" + path);
        }
    }

    private static class SingletonHolder {
        private static final FileUtils INSTANCE = new FileUtils();

        private SingletonHolder() {
        }
    }

    private class WabbitFileFilter implements FileFilter {
        private WabbitFileFilter() {
        }

        public boolean accept(File pathname) {
            return pathname.isDirectory() || FileUtils.this.isValidFile(pathname.getPath());
        }
    }

    public static FileUtils getInstance() {
        return SingletonHolder.INSTANCE;
    }

    protected FileUtils() {
        startFileSearch();
    }

    public void invalidateFiles() {
        startFileSearch();
    }

    private void startFileSearch() {
        new AsyncTask<Void, Void, Void>() {
            protected void onPreExecute() {
                FileUtils.this.mSearchLatch = new CountDownLatch(1);
            }

            public Void doInBackground(Void... params) {
                if (StorageUtils.hasExternalStorage()) {
                    FileUtils.this.mFiles = FileUtils.this.findValidFiles(StorageUtils.getPrimaryStoragePath());
                    String extraStorage = System.getenv("SECONDARY_STORAGE");
                    String extraStorageMore = System.getenv("EMULATED_STORAGE_TARGET");
                    if (!(extraStorage == null || "".equals(extraStorage) || extraStorageMore == null || "".equals(extraStorageMore))) {
                        for (String dir : extraStorage.split(":")) {
                            FileUtils.this.mFiles.addAll(FileUtils.this.findValidFiles(dir));
                        }
                    }
                } else {
                    FileUtils.this.mFiles = new HashSet();
                }
                FileUtils.this.mSearchLatch.countDown();
                return null;
            }

            protected void onPostExecute(Void arg) {
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public List<String> getValidFiles(String extensionsRegex) {
        try {
            this.mSearchLatch.await();
            List<String> arrayList = new ArrayList();
            Pattern pattern = Pattern.compile(extensionsRegex, 2);
            for (String file : this.mFiles) {
                if (pattern.matcher(file).find()) {
                    arrayList.add(file);
                }
            }
            return arrayList;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList();
        }
    }

    private Set<String> findValidFiles(String dir) {
        Set<String> validFiles = new HashSet();
        File[] rootDirArray = new File(dir).listFiles();
        if (rootDirArray != null) {
            LinkedList<File> filesToSearch = new LinkedList(Arrays.asList(rootDirArray));
            while (!filesToSearch.isEmpty()) {
                File file = (File) filesToSearch.removeFirst();
                if (file.isDirectory()) {
                    FileObserver observer = new SingleFileObserver(file.getPath(), 768);
                    this.mObservers.add(observer);
                    observer.startWatching();
                    File[] subDirArray = file.listFiles(new WabbitFileFilter());
                    if (!(subDirArray == null || subDirArray.length == 0)) {
                        Collections.addAll(filesToSearch, subDirArray);
                    }
                } else if (isValidFile(file.getPath())) {
                    validFiles.add(file.getAbsolutePath());
                }
            }
        }
        return validFiles;
    }

    private boolean isValidFile(String file) {
        int i = file.lastIndexOf(46);
        if (i <= 0 || file.length() != i + 4) {
            return false;
        }
        i++;
        char ext1 = Character.toLowerCase(file.charAt(i));
        i++;
        char ext2 = Character.toLowerCase(file.charAt(i));
        char ext3 = Character.toLowerCase(file.charAt(i + 1));
        if ((ext1 == 'r' && ext2 == 'o' && ext3 == 'm') || (ext1 == 's' && ext2 == 'a' && ext3 == 'v')) {
            return true;
        }
        if (ext1 != '8' && ext1 != '7') {
            return false;
        }
        switch (ext2) {
            case AdSize.PORTRAIT_AD_HEIGHT /*50*/:
            case '5':
            case '6':
            case 'c':
            case 'x':
                switch (ext3) {
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'g':
                    case 'i':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'p':
                    case 'q':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    case 'y':
                    case 'z':
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    private void handleFileEvent(int event, String path) {
        switch (event) {
            case 256:
                File file = new File(path);
                if (file.isFile()) {
                    this.mFiles.add(file.getAbsolutePath());
                    return;
                }
                return;
            case 512:
                this.mFiles.remove(path);
                return;
            default:
                return;
        }
    }
}
