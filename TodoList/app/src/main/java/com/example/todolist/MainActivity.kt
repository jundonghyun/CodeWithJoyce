package com.example.todolist

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityAddTodoActivitiyBinding
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.db.AppDatabase
import com.example.todolist.db.ToDoEntity
import com.example.todolist.db.TodoDao

class MainActivity : AppCompatActivity(), OnItemLongClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var todoDao: TodoDao
    private lateinit var todoList: ArrayList<ToDoEntity>
    private lateinit var adapter: TodoRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, AddTodoActivitiy::class.java)
            startActivity(intent)
        }

        db = AppDatabase.getInstance(this)!!
        todoDao = db.getTodoDao()

        getAllTodoList()

    }

    private fun getAllTodoList(){
        Thread{
            todoList = ArrayList(todoDao.getAll())
            setRecyclerView()
        }.start()
    }

    private fun setRecyclerView(){
        runOnUiThread{
            adapter = TodoRecyclerViewAdapter(todoList, this)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    override fun onRestart() {
        super.onRestart()
        getAllTodoList()
    }

    override fun onLongClick(position: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("할 일 삭제")
        builder.setMessage("정말 삭제하시겠습니까?")
        builder.setNegativeButton("취소", null)
        builder.setPositiveButton("네",
            object: DialogInterface.OnClickListener{
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    deleteTodo(position)
                }
            })
        builder.show()
    }

    private fun deleteTodo(position: Int){
        Thread{
            todoDao.deleteTodo(todoList[position])
            todoList.removeAt(position)
            runOnUiThread {
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}