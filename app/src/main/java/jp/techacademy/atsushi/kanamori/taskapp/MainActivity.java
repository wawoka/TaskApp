package jp.techacademy.atsushi.kanamori.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public final static String EXTRA_TASK = "jp.techacademy.atsushi.kanamori.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;


    private String[] array_adapter_data = {};  //TODO



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SearchView search = (SearchView) findViewById(R.id.searchView1);
        mListView = (ListView) findViewById(R.id.listView1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, array_adapter_data);  // TODO
        mListView.setAdapter(adapter);
        mListView.setTextFilterEnabled(true);

        // SearchViewの初期表示状態を設定
        search.setIconifiedByDefault(false);

        // SearchViewにOnQueryChangeListenerを設定
        search.setOnQueryTextListener(this);

        // SearchViewのSubmitボタンを使用不可にする
        search.setSubmitButtonEnabled(true);

        // SearchViewに何も入力していない時のテキストを設定
        search.setQueryHint("検索文字を入力して下さい。");



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }

    public boolean onQueryTextChange(String newText) {   //TODO
        if (TextUtils.isEmpty(newText)) {
            mListView.clearTextFilter();
        } else {
            mListView.setFilterText(newText.toString());
        }
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }



    private void reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        RealmResults<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }
}