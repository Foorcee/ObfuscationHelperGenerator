package de.foorcee.mappings.utils;

public class Utils {

    public static String getSimpleName(String name, char s){
        int index = name.indexOf(s);
        return name.substring(index +1);
    }

    public boolean test2(){
        return true;
    }

    public boolean test(){
        return this.test2();
    }

    public void main2(){

    }

    public void main(){
        main2();
    }

    public void clean(long l, long l2, long l3, long l4){

    }

    public void clean2(long l, long l2, int l3, int l4){
        clean(l, l2, l3, l4);
    }
}
