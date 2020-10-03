package de.foorcee.mappings.data.mojang;

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

    public void clean(boolean b, String s){

    }

    public void clean2(boolean b, String s){
        clean(b, s);
    }
}
