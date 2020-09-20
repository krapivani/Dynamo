package edu.buffalo.cse.cse486586.simpledynamo;

import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


public class SimpleDynamoProvider extends ContentProvider {

	static final int SERVER_PORT = 10000;
	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	private Uri mUri = null;
	String myPort = "";
    HashMap<String,String> joind = new HashMap<String, String>();
    HashMap<String,String> returnq = new HashMap<String,String>();
	ArrayList<String> ports = new ArrayList<String>();
	ArrayList<String> inserted = new ArrayList<String>();
	ArrayList<String> avds = new ArrayList<String>();
    String [] orderedJoind = new String[5];
	boolean returned= false;
    Object insertSync = new Object();
    Object querySync = new Object();

	public boolean onCreate() {

		ports.add(REMOTE_PORT0);
		ports.add(REMOTE_PORT1);
		ports.add(REMOTE_PORT2);
		ports.add(REMOTE_PORT3);
		ports.add(REMOTE_PORT4);

		TelephonyManager tel0 = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr0 = tel0.getLine1Number().substring(tel0.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr0)));

		try {

			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

		} catch (IOException e) {

			Log.e("SERVER", "Can't create a ServerSocket");
			return false;

		}

		String msg = "CHECKIN:" + myPort;
		try {
			joind.put(myPort,genHash(myPort));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		avds.add(myPort);

		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, "5554");

		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		String[] s = selection.split(":");

		if(selection.equals("*")){

			for(int i=0;i<avds.size();i++){
				String msg = "DELETEALL:";
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, avds.get(i));

			}
			inserted = new ArrayList<String>();
		}

		else if(selection.equals("@")){

			for(int i = 0;i <inserted.size(); i++){
				File dir = getContext().getFilesDir();
				File file = new File(dir, inserted.get(i));
				file.delete();
			}
			inserted = new ArrayList<String>();
		}

		else if(s[0].equals("DELETEONE")){

			for(int i = 0;i <inserted.size(); i++){

				if(s[1].equals(inserted.get(i))){
					File dir = getContext().getFilesDir();
					File file = new File(dir, inserted.get(i));
					file.delete();
				}
			}
			inserted.remove(s[1]);
			return 0;
		}

		else {

			sortList();
			String check = null;
			try {
				check = genHash(selection);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

			String avdOne = "";
			for (int i = 0;i < orderedJoind.length;i++) {
				if (i == 0) {
					avdOne = orderedJoind[i].split("%")[0];
				}
				if ((check.compareTo(orderedJoind[i].split("%")[1]) < 0)) {

					if (orderedJoind[i].split("%")[0].equals(myPort)) {
						File dir = getContext().getFilesDir();
						File file = new File(dir, selection);
						file.delete();
						inserted.remove(selection);
                        String msgToSend = "DELETEONE:" + selection;
                        if(i == 3){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                        }
                        else if(i == 4){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
                        }
                        else{
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+2].split("%")[0]);
                        }

						return 0;
					} else {

						String msgToSend = "DELETEONE:" + selection;
                        if(i == 3){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                        }
                        else if(i == 4){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
                        }
                        else {
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+2].split("%")[0]);
                        }
						return 0;

					}

				}
			}
			String msgToSend = "DELETEONE:" + selection;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, avdOne);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[2].split("%")[0]);
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		Set<Map.Entry<String, Object>> val1 = values.valueSet();
		Iterator it = val1.iterator();
		String set ="";
		String value = "";
		String key ="";

		while(it.hasNext()) {

			Map.Entry me = (Map.Entry) it.next();
			set = me.getKey().toString();
			if (set.equals("key")) {
				key = me.getValue().toString();
			}
			if (set.equals("value")) {
				value = me.getValue().toString();
			}
		}

		sortList();

		String check = null;
		try {
			check = genHash(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		//int h = 0;
		String avdOne = "";

		/* To get information on how to use synchronization
		https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html*/
		
		synchronized (insertSync){
            for (int i = 0; i < orderedJoind.length; i++){
                if(i == 0){
                    avdOne = orderedJoind[i].split("%")[0];
                }

                if ((check.compareTo(orderedJoind[i].split("%")[1]) < 0)) {

                    if (orderedJoind[i].split("%")[0].equals(myPort)) {
                        FileOutputStream outputStream;

                        try {
                            outputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
                            outputStream.write(value.getBytes());
                            outputStream.close();
                            inserted.add(key);
                        } catch (Exception e) {
                            Log.v("insert", "File write failed");
                            e.printStackTrace();
                        }
                        String msgToSend = "INSERT:" + key + ":" + value;
                        if(i == 3){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                        }
                        else if(i == 4){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
                        }
                        else{
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+2].split("%")[0]);
                        }

                        return null;


                    } else {
                        String msgToSend = "INSERT:" + key + ":" + value;

                        if(i == 3){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                        }
                        else if(i == 4){
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[0].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
                        }
                        else {

                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+1].split("%")[0]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i+2].split("%")[0]);
                        }
                        return null;
                    }

                }


            }
            String msgToSend = "INSERT:" + key + ":" + value;
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, avdOne);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[1].split("%")[0]);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[2].split("%")[0]);


        }


        return null;

	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,String sortOrder) {

		String[] s = selection.split(":");

		if(selection.equals("*")){
			String[] column = new String[2];
			column[0] = "key";
			column[1] = "value";
			MatrixCursor cursor = new MatrixCursor(column);

			for(int i=0; i<avds.size(); i++){
				String msg = "QUERYALL:" + myPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, avds.get(i));
				while(!returned){ }
				if(returned){
					for(Map.Entry<String,String> entry : returnq.entrySet()){
						String key = entry.getKey();
						String val = entry.getValue();
						String[] add = new String[2];
						add[0] = key;
						add[1] = val;
						cursor.addRow(add);
					}
				}
				returned = false;
			}
			return cursor;
		}

		else if(selection.equals("@")){
			String[] column = new String[2];
			column[0] = "key";
			column[1] = "value";
			MatrixCursor cursor = new MatrixCursor(column);
			for(int i = 0;i <inserted.size(); i++){
				FileInputStream fis = null;
				String text ="";
				try {
					fis = getContext().openFileInput(inserted.get(i));
					InputStreamReader isr = new InputStreamReader(fis);
					BufferedReader bufferedReader = new BufferedReader(isr);
					String line="";
					while ((line = bufferedReader.readLine()) != null) {
						text = text + line;
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException o) {
					o.printStackTrace();
				}
				String[] add = new String[2];
				add[0] = inserted.get(i);
				add[1] = text;
				cursor.addRow(add);
			}
			return cursor;
		}

		else {
            String[] column = new String[2];
            column[0] = "key";
            column[1] = "value";
            MatrixCursor cursor = new MatrixCursor(column);

            sortList();
            String check = null;
            try {
                check = genHash(selection);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            String avdOne = "";
            synchronized (querySync){

                for (int i = 0; i < orderedJoind.length; i++) {
                    if (i == 0) {
                        avdOne = orderedJoind[i].split("%")[0];
                    }

                    if ((check.compareTo(orderedJoind[i].split("%")[1]) < 0)) {
                        if (orderedJoind[i].split("%")[0].equals(myPort)) {
                            FileInputStream fis = null;
                            String text = "";
                            try {
                                fis = getContext().openFileInput(selection);
                                InputStreamReader isr = new InputStreamReader(fis);
                                BufferedReader bufferedReader = new BufferedReader(isr);
                                String line = "";
                                while ((line = bufferedReader.readLine()) != null) {
                                    text = text + line;
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException o) {
                                o.printStackTrace();
                            }
                            String[] add = new String[2];
                            add[0] = selection;
                            add[1] = text;
                            cursor.addRow(add);
                            return cursor;

                        } else {
                            String msgToSend = "QUERYONE:" + selection + ":" + myPort;
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, orderedJoind[i].split("%")[0]);
                            while (!returned) { }
                            if (returned) {
                                String val = returnq.get(selection);
                                String[] add = new String[2];
                                add[0] = selection;
                                add[1] = val;
                                cursor.addRow(add);
                                returned = false;
                                return cursor;
                            }
                        }
                    }
                }
                String msgToSend = "QUERYONE:" + selection + ":" + myPort;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, avdOne);
                while (!returned) {

                }
                if (returned) {
                    String val = returnq.get(selection);
                    String[] add = new String[2];
                    add[0] = selection;
                    add[1] = val;
                    cursor.addRow(add);
                    returned = false;
                    return cursor;

                }

            }

        }

		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}




	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	public void sortList(){

		LinkedList list = new LinkedList(joind.entrySet());

		Collections.sort(list, new Comparator() {

                /*Use this site for sorting HashMap in java by Values
                    https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/ */

			public int compare(Object lhs, Object rhs) {
				return ((Comparable) ((Map.Entry) (lhs)).getValue()).compareTo(((Map.Entry) (rhs)).getValue());
			}
		});

		//String [] sorted = new String[5];
        int index = 0;
		for (Iterator it = list.iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry) it.next();
			orderedJoind[index] = entry.getKey()+ "%" + entry.getValue();
			index++;
		}

	}


	class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... sockets) {


			while (true) {
				try {
					String text = "";
					ServerSocket serverSocket = sockets[0];
					Socket socket1 = serverSocket.accept();
					BufferedReader read = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
					String message;
					while ((message = read.readLine()) != null) {
						text += message;
					}

					Log.e("READ", text);


					String[] str = text.split(":");

					if (str[0].equals("CHECKIN")) {

						avds.add(str[1]);
						String avd = "AVD:";

						for (int i = 0; i < avds.size(); i++) {
							avd += avds.get(i) + ":";
						}

						for (int j = 0; j < avds.size(); j++) {

                            socketwrite(avds.get(j), avd);

						}
					}


					if(str[0].equals("AVD")){

						for (int y= 1;y<str.length;y++){
							if (!joind.containsKey(str[y])) {
								if(!avds.contains(str[y])){
									avds.add(str[y]);
								}
								joind.put(str[y], genHash(str[y]));
							}
						}

					}

					else if(str[0].equals("INSERT")){
						FileOutputStream outputStream;
						try {
							outputStream = getContext().openFileOutput(str[1], Context.MODE_PRIVATE);
							outputStream.write(str[2].getBytes());
							outputStream.close();
							inserted.add(str[1]);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}


					else if(str[0].equals("DELETEALL")){
						int delete = delete(mUri, "@", null);
					}

					else if(str[0].equals("DELETE")){
						int delete = delete(mUri, "@", null);
					}


					else if(str[0].equals("DELETEONE")){
						int delete = delete(mUri, "DELETEONE" + ":" +  str[1] , null);
					}

					else if(str[0].equals("QUERYALL")){

					    String ret = "RETURN:";
						for(int i = 0;i <inserted.size(); i++){

							FileInputStream fis = null;
							try {
								String t ="";
								fis = getContext().openFileInput(inserted.get(i));
								InputStreamReader isr = new InputStreamReader(fis);
								BufferedReader bufferedReader = new BufferedReader(isr);
								String line="";
								while ((line = bufferedReader.readLine()) != null) {
									t = t + line;
								}
								ret += inserted.get(i) + "%" + t + ":";
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException o) {
								o.printStackTrace();
							}
						}
                        socketwrite(str[1], ret);
					}

					else if (str[0].equals("QUERYONE")) {
                        FileInputStream fis = null;
                        String t = "";
                        try {
                            fis = getContext().openFileInput(str[1]);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader bufferedReader = new BufferedReader(isr);
                            String line = "";
                            while ((line = bufferedReader.readLine()) != null) {
                                t = t + line;
                            }
                        } catch (SocketTimeoutException e) {
                            Log.v("EXE","Server");
                            e.printStackTrace();
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException o) {
                            o.printStackTrace();
                        }
                        String ret = "RETURN:" + str[1] + "%" + t;
                        socketwrite(str[2], ret);
					}
					else if(str[0].equals("RETURN")){

						returnq = new HashMap<String, String>();

						for(int o = 1; o<str.length; o++){
							String [] l = str[o].split("%");
							returnq.put(l[0],l[1]);
						}
						returned = true;
					}

					//publishProgress(text);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
		}




		private void socketwrite(String port,String msg){

            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port) * 2);
                socket.setSoTimeout(5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.write(msg);
                out.close();
            }catch(SocketTimeoutException s1){
                Log.e("SERVER", "SocketTimeout exeception");
            }catch (UnknownHostException e) {
                Log.e("Server", "avd");
            } catch (IOException e) {
                Log.e("Server", "avd");
            }
        }


		protected void onProgressUpdate(String... strings) {

			return;
		}
	}

	class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {


			//int p = Integer.parseInt(msgs[1]);
			//p = p*2;

			try {

				Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(msgs[1])*2);
				String msgToSend = msgs[0];
				socket0.setSoTimeout(5000);
				PrintWriter out0 = new PrintWriter(socket0.getOutputStream(), true);
				out0.write(msgToSend);
				out0.close();
				socket0.close();

			}catch(SocketTimeoutException s1){
				Log.e("CLIENT", "SocketTimeout exeception");
			} catch (UnknownHostException e) {
				Log.e("CLIENT", "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e("CLIENT", "ClientTask socket IOException");
			}

			return null;
		}
	}
}



