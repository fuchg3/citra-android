package org.citra.citra_emu.utils;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.citra.citra_emu.model.GameDatabase;
import org.citra.citra_emu.model.GameProvider;

import java.lang.ref.WeakReference;

public class AddDirectoryHelper {
    private final ContentResolver mContentResolver;

    public AddDirectoryHelper(Context context) {
        mContentResolver = context.getContentResolver();
    }

    private static class AddDirectoryHandler extends AsyncQueryHandler {
        private final WeakReference<AddDirectoryListener> mListenerWeakReference;

        public AddDirectoryHandler(ContentResolver contentResolver, AddDirectoryListener listener) {
            super(contentResolver);
            mListenerWeakReference = new WeakReference<>(listener);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            mListenerWeakReference.get().onDirectoryAdded();
        }
    }

    public void addDirectory(String dir, AddDirectoryListener addDirectoryListener) {
        AsyncQueryHandler handler = new AddDirectoryHandler(mContentResolver, addDirectoryListener);

        ContentValues file = new ContentValues();
        file.put(GameDatabase.KEY_FOLDER_PATH, dir);

        handler.startInsert(0,                // We don't need to identify this call to the handler
                null,                        // We don't need to pass additional data to the handler
                GameProvider.URI_FOLDER,    // Tell the GameProvider we are adding a folder
                file);
    }

    public interface AddDirectoryListener {
        void onDirectoryAdded();
    }
}
