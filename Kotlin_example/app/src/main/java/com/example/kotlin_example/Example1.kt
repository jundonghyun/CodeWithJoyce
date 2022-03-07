import java.lang.IllegalArgumentException

fun main(){
    val newPerson: Person("")
}

class Person(name: String){
    val name: String
    init {
        if(name.isEmpty()){
            throw IllegalArgumentException("이름이 없어요")
        }
        this.name = name
    }
}