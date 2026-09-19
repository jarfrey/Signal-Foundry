// package predictbackend;// max

// import java.io.*;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.ArrayList;

// import org.json.*;

// public class Parser {

//     private String str;
//     private final JSONObject json;
//     private final JSONArray jobs;


//     public Parser (String filename) throws IOException{
//         // get the file
//         Path name = Path.of(filename);
//         // read the file to a string
//         str = Files.readString(name);
//         // turn the string into a JSONObject for parsing
//         json = new JSONObject(str);
//         // get the array of job entries
//         jobs = json.getJSONArray("jobs");
//     }

//     // returns arraylist of JSONObjects
//     public ArrayList<JSONObject> split_JSONObject() {
//         // convert from JSONArray to ArrayList
//         ArrayList<JSONObject> split_array = new ArrayList<>(jobs.length());
//         for (int i = 0; i < jobs.length(); i++) {
//             split_array.add(jobs.getJSONObject(i));
//         }
//         return split_array;
//     }

//     // return arraylist of strings
//     public ArrayList<String> split_String() {
//         // convert from jsonobject to string
//         ArrayList<String> split_array = new ArrayList<>(jobs.length());
//         for (int i = 0; i < jobs.length(); i++) {
//             split_array.add(jobs.getJSONObject(i).toString());
//         }
//         return split_array;
//     }

//     // pass key and block
//     // returns the value of the requested key as a string
//     public String getString(String key, String jsonBlock) {
//         // convert given block to usable json object
//         JSONObject block = new JSONObject(jsonBlock);

//         return block.get(key).toString();
//     }
    
//     // pass key and block
//     // does the same thing as getString() but int
//     public int getInt(String key, String jsonBlock) {
//         JSONObject block = new JSONObject(jsonBlock);

//         return block.getInt(key);
//     }
// }
