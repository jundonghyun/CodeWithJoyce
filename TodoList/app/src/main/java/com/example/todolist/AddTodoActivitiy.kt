package com.example.todolist

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.todolist.databinding.ActivityAddTodoActivitiyBinding
import com.example.todolist.db.AppDatabase
import com.example.todolist.db.ToDoEntity
import com.example.todolist.db.TodoDao
import kotlin.concurrent.thread

class AddTodoActivitiy : AppCompatActivity() {

    private lateinit var binding: ActivityAddTodoActivitiyBinding
    lateinit var db: AppDatabase
    lateinit var todoDao: TodoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTodoActivitiyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)!!
        todoDao = db.getTodoDao()

        binding.btnCompletion.setOnClickListener {
            insertTodo()
        }
    }

    private fun insertTodo(){
        val todoTitle = binding.edtTitle.text.toString()
        var todoImportance = binding.radioGroup.checkedRadioButtonId

        when(todoImportance){
            R.id.btn_high -> {
                todoImportance = 1
            }

            R.id.btn_middle -> {
                todoImportance = 2
            }

            R.id.btn_low -> {
                todoImportance = 3
            }

            else -> {
                todoImportance = -1
            }
        }

        if(todoImportance == -1 || todoTitle.isBlank()){
            Toast.makeText(this,"모든항목을 채워주세요", Toast.LENGTH_SHORT).show()
        }
        else{
            Thread{ //코루틴으로 사용해 볼것
                todoDao.insertTodo(ToDoEntity(null, todoTitle, todoImportance))
                runOnUiThread {
                    Toast.makeText(this, "추가되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.start()
        }
    }
}