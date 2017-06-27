package com.mobgen.droidcon.offline.sdk.repository.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.mobgen.droidcon.offline.sdk.base.DatabaseException;
import com.mobgen.droidcon.offline.sdk.base.DatabaseManager;
import com.mobgen.droidcon.offline.sdk.model.db.CommentModel;
import com.mobgen.droidcon.offline.sdk.models.Comment;
import com.mobgen.droidcon.offline.sdk.models.Post;
import com.mobgen.droidcon.offline.sdk.models.db.CommentDb;

import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    private DatabaseManager mDatabaseManager;

    public CommentDAO(DatabaseManager databaseManager) {
        mDatabaseManager = databaseManager;
    }

    @WorkerThread
    public List<Comment> comments(@Nullable final Long postId) {
        final List<Comment> comments = new ArrayList<>();
        if (postId != null) {
            try {
                mDatabaseManager.transaction(new DatabaseManager.Transaction() {
                    @Override
                    public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                        try (Cursor cursor = database.rawQuery(CommentModel.SELECTCOMMENTSPOST, new String[]{String.valueOf(postId)})) {
                            while (cursor.moveToNext()) {
                                comments.add(CommentDb.COMMENTS_POST_MAPPER.map(cursor).asModel());
                            }
                        }
                    }
                });
            } catch (DatabaseException e) {
                Log.e("Database", "Error in the database", e);
            }
        }
        return comments;
    }

    @WorkerThread
    public void save(@NonNull final Comment comment) throws DatabaseException {
        mDatabaseManager.transaction(new DatabaseManager.Transaction() {
            @Override
            public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                database.insertWithOnConflict(CommentModel.TABLE_NAME, null, comment.marshal(), SQLiteDatabase.CONFLICT_REPLACE);
            }
        });
    }

    @WorkerThread
    public void save(@NonNull final Post post, @NonNull final List<Comment> comments) throws DatabaseException {
        mDatabaseManager.transaction(new DatabaseManager.Transaction() {
            @Override
            public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                //Could be more efficient by reusing the statement, but what the hell
                for (Comment comment : comments) {
                    database.insertWithOnConflict(CommentModel.TABLE_NAME, null,
                            Comment.builder(comment)
                                    .internalPostId(post.internalId())
                                    .build()
                                    .marshal(), SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
        });
    }

    @NonNull
    @WorkerThread
    public List<Comment> commentsPendingToSync() throws DatabaseException {
        final List<Comment> comments = new ArrayList<>();
        mDatabaseManager.transaction(new DatabaseManager.Transaction() {
            @Override
            public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                try (Cursor cursor = database.rawQuery(CommentModel.SELECTSYNCCOMMENTS, null)) {
                    while (cursor.moveToNext()) {
                        comments.add(CommentDb.SYNC_POSTS_MAPPER.map(cursor).asModel());
                    }
                }
            }
        });
        return comments;
    }

    @WorkerThread
    public void delete(@Nullable final Long commentId) throws DatabaseException {
        if (commentId != null) {
            mDatabaseManager.transaction(new DatabaseManager.Transaction() {
                @Override
                public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                    database.execSQL(CommentModel.DELETECOMMENT, new Long[]{commentId});
                }
            });
        }
    }

    @WorkerThread
    public void deleteFromPost(@Nullable final Post post) throws DatabaseException {
        if (post != null) {
            mDatabaseManager.transaction(new DatabaseManager.Transaction() {
                @Override
                public void onTransaction(@NonNull SQLiteDatabase database) throws DatabaseException {
                    database.execSQL(CommentModel.DELETECOMMENTSBYPOST, new Long[]{post.internalId()});
                }
            });
        }
    }
}
