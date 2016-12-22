package io.github.zhaomy6.lifebattery;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class PlansActivity extends AppCompatActivity {
    private ListView listView;
    private MyDB myDB;
    private SimpleCursorAdapter sca;

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.addAction) {
                Intent intent = new Intent();
                intent.setClass(PlansActivity.this, AddActivity.class);
                startActivity(intent);
                return true;
            } else if (menuItem.getItemId() == R.id.sortAction) {
                Cursor cursors = myDB.sortWithTime();
                sca.swapCursor(cursors);
            }
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.searchAction).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent intent = new Intent();
                intent.setClass(PlansActivity.this, HandleSearchActivity.class);
                intent.putExtra("query", query);
                startActivityForResult(intent, 1);
//                Toast.makeText(PlansActivity.this, query, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    public void updateListView() {
        Cursor cursors = myDB.getPart();
        sca.swapCursor(cursors);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                updateListView();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolBar);
        toolbar.setTitle("LifeBattery");
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(onMenuItemClick);

        myDB = new MyDB(this);
        Cursor listItems = myDB.getPart();
        sca = new SimpleCursorAdapter(getApplicationContext(), R.layout.plans_item,
                listItems, new String[] {"title", "DDL"},
                new int[]{R.id.planTitle, R.id.planDDL}, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        listView = (ListView) findViewById(R.id.planList);
        listView.setAdapter(sca);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            private String titleText;
            private String DDLText;
            private String detailText;
            private String timeTextToShow;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LayoutInflater factory = LayoutInflater.from(PlansActivity.this);
                View views = factory.inflate(R.layout.dialoglayout, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(PlansActivity.this);
                builder.setView(views);

                TextView d_planType = (TextView)views.findViewById(R.id.d_planType);
                TextView d_planDDL = (TextView)views.findViewById(R.id.d_planDDL);
                TextView d_planDetail = (TextView)views.findViewById(R.id.d_planDetail);

                Cursor cursor = (Cursor)sca.getItem(position);
                titleText = cursor.getString(cursor.getColumnIndex("title"));
                DDLText = cursor.getString(cursor.getColumnIndex("DDL"));
                timeTextToShow = "";

                Intent intent = new Intent("widgetReceiver");
                intent.setClass(PlansActivity.this, WidgetReceiver.class);
                intent.putExtra("DDL", DDLText);
                intent.putExtra("title", titleText);
                sendBroadcast(intent);

                String[] frag = DDLText.split("\n");
                timeTextToShow = "截止日期:\n" + frag[0] + " " + frag[1];

                d_planDDL.setText(timeTextToShow);

                Cursor cursor1 = myDB.getWithTitle(titleText);
                cursor1.moveToFirst();

                detailText = cursor1.getString(cursor1.getColumnIndex("detail"));
                d_planDetail.setText(detailText);

                String typeText = "任务类型类型: " + "近期计划";
                d_planType.setText(typeText);

                //  对话框属性
                builder.setTitle(titleText);
                builder.setNegativeButton("修改任务", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClass(PlansActivity.this, AddActivity.class);
//                        intent.putExtra("intent", "modify added plan");
                        intent.putExtra("titleText", titleText);
                        intent.putExtra("DDLText", DDLText);
                        intent.putExtra("detailText", detailText);
                        startActivityForResult(intent, 1);
                    }
                });
                builder.setPositiveButton("确定", null);

                builder.create().show();
            }

        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Cursor cursor = (Cursor)sca.getItem(position);
                final String title = cursor.getString(cursor.getColumnIndex("title"));
                final String DDL = cursor.getString(cursor.getColumnIndex("DDL"));
                AlertDialog.Builder builder = new AlertDialog.Builder(PlansActivity.this);
                builder.setTitle("任务管理");

                builder.setNegativeButton("取消任务", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myDB.deleteDB(title);
                        updateListView();
                    }
                });

                builder.setPositiveButton("完成任务",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Date currentTime = new Date();
//                        String formatString = "yyyy-MM-dd HH:mm a";
//                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatString, Locale.CHINA);
//                        String res = simpleDateFormat.format(currentTime);
//                        Toast.makeText(PlansActivity.this, res, Toast.LENGTH_SHORT).show();
//
//                        String[] frag = DDL.split("\n");
//                        String DDLFormat = frag[0] + " " + frag[1];
//                        if (DDLFormat.compareTo(res) == -1) {
//
//                        }
//                        boolean fuck = DateFormat.is24HourFormat(getApplicationContext());

                        myDB.updateFinished(title, "完成");
                        updateListView();
                    }
                });
                builder.create().show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sca.swapCursor(null);
    }
}
