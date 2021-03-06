package com.example.lmont.iceicebb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Random;

/**
 * Created by lmont on 9/25/2016.
 */
public class IcebreakerDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "icebreakerDB";
    public static final int DB_VERSION = 12;
    public static final String ICEBREAKERS_TABLE_NAME = "icebreakers";
    public static final String QUESTIONS_TABLE_NAME = "questions";
    public static final String COMMENTS_TABLE_NAME = "comments";

    public static final String[] ICEBREAKERS_COLUMNS = new String[]{"name", "comment", "rules", "isclean", "hasDice", "hasCards", "tags", "minPlayers", "maxPlayers", "materials", "url", "rating"};
    public static final String[] QUESTIONS_COLUMNS = new String[]{"name", "text", "sfw"};
    public static final String[] COMMENTS_COLUMNS = new String[]{"gameName", "userName", "text", "rating"};

    // CREATE TABLE [TABLE NAME] ([ATTRIBUTE NAME] [ATTRIBUTE TYPE], ...)
    public static final String CREATE_ICEBREAKERS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + ICEBREAKERS_TABLE_NAME + " (" +
            "name INTEGER," +
            "comment TEXT," +
            "rules TEXT," +
            "isclean BOOLEAN," +
            "hasDice BOOLEAN," +
            "hasCards BOOLEAN," +
            "tags TEXT," +
            "minPlayers INTEGER," +
            "maxPlayers INTEGER," +
            "materials TEXT," +
            "url TEXT," +
            "rating INTEGER" +
            //", PRIMARY KEY (name)"+
            ")";

    public static final String CREATE_QUESTIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + QUESTIONS_TABLE_NAME + " (" +
            "name TEXT," +
            "text TEXT," +
            "sfw BOOLEAN"+
            //", PRIMARY KEY (name)" +
            ")";

    public static final String CREATE_COMMENTS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + COMMENTS_TABLE_NAME + " (" +
            "gameName TEXT," +
            "userName TEXT," +
            "text TEXT," +
            "rating INTEGER"+
            ")";

    public static IcebreakerDBHelper instance;

    private IcebreakerDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static IcebreakerDBHelper getInstance(Context context) {
        if (instance == null)
            instance = new IcebreakerDBHelper(context);

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_ICEBREAKERS_TABLE);
        sqLiteDatabase.execSQL(CREATE_QUESTIONS_TABLE);
        sqLiteDatabase.execSQL(CREATE_COMMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ICEBREAKERS_TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QUESTIONS_TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + COMMENTS_TABLE_NAME);
        this.onCreate(sqLiteDatabase);
    }

    public void deleteAllComments() {
        getWritableDatabase().delete(COMMENTS_TABLE_NAME,null,null);
    }

    public void addComment(ContentValues cv) {
        Cursor c = null;
        String query = "select * from " + COMMENTS_TABLE_NAME +
                " where userName = \"" + cv.getAsString("userName") + "\" " +
                " AND gameName = \"" + cv.getAsString("gameName") + "\" " +
                " AND text = \"" + cv.getAsString("text") + "\" ";
        c = getReadableDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            return;
        }
        getWritableDatabase().insert(COMMENTS_TABLE_NAME, null, cv);
    }

    public Game.Comment[] getCommentsForGame(String gameName) {
        String selection = "gameName = \""+gameName+"\"";

        Cursor cursor = getReadableDatabase().query(
                COMMENTS_TABLE_NAME,
                COMMENTS_COLUMNS,
                selection, null, null, null, null);;

        Game.Comment[] comments = new Game.Comment[cursor.getCount()];

        for (int x=0; x<comments.length; x++) {
            cursor.moveToPosition(x);
            comments[x] = new Game.Comment();
            comments[x].gameName = cursor.getString(cursor.getColumnIndex("gameName"));
            comments[x].userName = cursor.getString(cursor.getColumnIndex("userName"));
            comments[x].text = cursor.getString(cursor.getColumnIndex("text"));
            comments[x].rating = cursor.getInt(cursor.getColumnIndex("rating"));
        }

        return comments;
    }

    public Game getGameWithName(String query) {
        Cursor cursor = getReadableDatabase().query(
                ICEBREAKERS_TABLE_NAME,
                ICEBREAKERS_COLUMNS,
                "name = \"" + query + "\"", null, null, null, null);

        if (cursor.moveToFirst() == false)
            return null;

        Game game = new Game();
        game.name = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[0]));
        game.comment = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[1]));
        game.rules = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[2]));
        game.isclean = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[3])) > 0;
        game.hasDice = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[4])) > 0;
        game.hasCards = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[5])) > 0;
        game.tags = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[6]));
        game.minPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[7]));
        game.maxPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[8]));
        game.materials = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[9]));
        game.url = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[10]));
        game.rating = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[11]));

        int ratingAvg = game.rating;
        Game.Comment[] comments = getCommentsForGame(game.name);
        for(Game.Comment comment : comments) {
            ratingAvg += comment.rating;
        }

        game.rating = ratingAvg / (comments.length + 1);

        return game;
    }

    public Game[] getGamesLike(String query, String tagsQuery, boolean isCleanQuery, boolean isByRatingQuery, boolean isByAlphabetQuery) {

        String selection = "name LIKE \"%"+query+"%\" AND tags LIKE \"%"+tagsQuery+"%\"";
        selection = isCleanQuery ? selection + " AND isclean = 1" : selection;
        String[] selectionArgs = new String[]{};
        String orderBy = null;
        orderBy = isByRatingQuery ? "rating DESC" : isByAlphabetQuery ? "name" : null;

        Cursor cursor = getReadableDatabase().query(
                ICEBREAKERS_TABLE_NAME,
                ICEBREAKERS_COLUMNS,
                selection, selectionArgs, null, null, orderBy);

        Game[] games = new Game[cursor.getCount()];

        for (int x=0; x<games.length; x++) {
            cursor.moveToPosition(x);
            games[x] = new Game();
            games[x].name = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[0]));
            games[x].comment = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[1]));
            games[x].rules = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[2]));
            games[x].isclean = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[3])) > 0;
            games[x].hasDice = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[4])) > 0;
            games[x].hasCards = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[5])) > 0;
            games[x].tags = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[6]));
            games[x].minPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[7]));
            games[x].maxPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[8]));
            games[x].materials = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[9]));
            games[x].url = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[10]));
            games[x].rating = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[11]));

            int ratingAvg = games[x].rating;
            Game.Comment[] comments = getCommentsForGame(games[x].name);
            for(Game.Comment comment : comments) {
                ratingAvg += comment.rating;
            }

            games[x].rating = ratingAvg / (comments.length + 1);
        }

        return games;
    }

    public void addGame(ContentValues cv) {
        Cursor c = null;
        String query = "select * from " + ICEBREAKERS_TABLE_NAME + " where name = \"" + cv.getAsString("name") + "\"";
        c = getReadableDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            return;
        }
        getWritableDatabase().insert(ICEBREAKERS_TABLE_NAME, null, cv);
    }

    public Game[] getAllGames() {
        Cursor cursor = getReadableDatabase().query(
                ICEBREAKERS_TABLE_NAME,
                ICEBREAKERS_COLUMNS,
                null, null, null, null, null);

        Game[] games = new Game[cursor.getCount()];

        for (int x=0; x<games.length; x++) {
            cursor.moveToPosition(x);
            games[x] = new Game();
            games[x].name = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[0]));
            games[x].comment = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[1]));
            games[x].rules = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[2]));
            games[x].isclean = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[3])) > 0;
            games[x].hasDice = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[4])) > 0;
            games[x].hasCards = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[5])) > 0;
            games[x].tags = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[6]));
            games[x].minPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[7]));
            games[x].maxPlayers = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[8]));
            games[x].materials = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[9]));
            games[x].url = cursor.getString(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[10]));
            games[x].rating = cursor.getInt(cursor.getColumnIndex(ICEBREAKERS_COLUMNS[11]));
        }

        return games;
    }

    public void deleteAllGames() {
        getWritableDatabase().delete(ICEBREAKERS_TABLE_NAME,null,null);
    }

    public void addQuestion(ContentValues cv) {
        Cursor c = null;
        String query = "select * from " + QUESTIONS_TABLE_NAME + " where name = \"" + cv.getAsString("name") + "\"";
        c = getReadableDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            return;
        }
        getWritableDatabase().insertOrThrow(QUESTIONS_TABLE_NAME, null, cv);
    }

    /**
     * isSFW : 1=true, 0=false, -1=false
     * @param isSFW
     * @return
     */
    public Game.Question getRandomQuestion(int isSFW) {

        String selection = null;

        if (isSFW == 1) {
            selection = "sfw = 1";
        }

        if (isSFW == 0) {
            selection = "sfw = 0";
        }

        Cursor cursor = getReadableDatabase().query(
                QUESTIONS_TABLE_NAME,
                QUESTIONS_COLUMNS,
                selection, null, null, null, null);

        int randomNum = (new Random()).nextInt(cursor.getCount());
        cursor.moveToPosition(randomNum);

        Game.Question question = new Game.Question();

        question.name = cursor.getString(cursor.getColumnIndex(QUESTIONS_COLUMNS[0]));
        question.text = cursor.getString(cursor.getColumnIndex(QUESTIONS_COLUMNS[1]));
        question.sfw = cursor.getInt(cursor.getColumnIndex(QUESTIONS_COLUMNS[2])) > 0;

        return question;
    }

    public void deleteAllQuestions() {
        getWritableDatabase().delete(QUESTIONS_TABLE_NAME,null,null);
    }

    public int getGamesTableSize() {
        return getAllGames().length;
    }

    public void resetDB() {
        onUpgrade(getWritableDatabase(), 0, 0);
    }
}
