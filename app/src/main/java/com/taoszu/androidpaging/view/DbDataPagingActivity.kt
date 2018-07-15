package com.taoszu.androidpaging.view

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import com.taoszu.androidpaging.ArticleAllAdapter
import com.taoszu.androidpaging.ArticlePageAdapter
import com.taoszu.androidpaging.R
import com.taoszu.androidpaging.db.ArticleDatabase
import com.taoszu.androidpaging.entity.ArticleEntity
import com.taoszu.androidpaging.ioThread
import kotlinx.android.synthetic.main.act_db_data_paging.*

class DbDataPagingActivity : AppCompatActivity(), View.OnClickListener {
    var postList: LiveData<PagedList<ArticleEntity>> ? = null
    lateinit var dataBase: ArticleDatabase

    val pageAdapter = ArticlePageAdapter()
    val allAdapter = ArticleAllAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_db_data_paging)
        dataBase = Room.databaseBuilder(this.applicationContext, ArticleDatabase::class.java, "db_article").build()

        recycle_article.layoutManager = LinearLayoutManager(this)

        btn_save.setOnClickListener(this)
        btn_page_show.setOnClickListener(this)
        btn_all_show.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_save -> {
                val articleDao = dataBase.articleDao()
                ioThread {
                    articleDao.insertAll(dataBase.genArticleList(articleDao))
                    runOnUiThread {
                        Toast.makeText(this, R.string.db_data_page_add_success_tip, Toast.LENGTH_LONG).show()
                    }
                }
            }
            R.id.btn_page_show -> {
                recycle_article.adapter = pageAdapter
                val articleDao = dataBase.articleDao()
                val pagedListConfig = PagedList.Config.Builder().setEnablePlaceholders(true).setPageSize(5).build()
                postList = LivePagedListBuilder(articleDao.getAllByDataSource(), pagedListConfig).build()
                postList!!.observe(this, Observer {
                    val startTime = System.currentTimeMillis()
                    pageAdapter.submitList(it)
                    value_render_time.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            val endTime = System.currentTimeMillis()
                            val time = endTime - startTime
                            value_render_time.text = getString(R.string.db_data_page_render_time_format, time.toString())
                            value_render_time.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    })
                    value_render_time.text = "-"
                })
            }
            R.id.btn_all_show -> {
                recycle_article.adapter = allAdapter
                val articleDao = dataBase.articleDao()
                ioThread {
                    allAdapter.dataList = articleDao.getAll()
                    runOnUiThread {
                        val startTime = System.currentTimeMillis()
                        allAdapter.notifyDataSetChanged()
                        value_render_time.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                val endTime = System.currentTimeMillis()
                                val time = endTime - startTime
                                Log.e("addTime", time.toString())
                                value_render_time.text = getString(R.string.db_data_page_render_time_format, time.toString())
                                value_render_time.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                        value_render_time.viewTreeObserver.addOnDrawListener {
                            val endTime = System.currentTimeMillis()
                            val time = endTime - startTime
                            Log.e("endTime", time.toString())
                        }
                        value_render_time.text = "-"
                    }
                }
            }
        }
    }



}
