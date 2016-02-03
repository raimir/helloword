import lotus.domino.*;
import xtr.dados.*;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class sendEmail  {//extends AgentBase
	
	private static Session session;
	private Database dbInstalacao = null;
	private static Document docFila = null;
	private static Document docAuxFila = null;
	public static int cont = 0;
	
	//public void NotesMain() {
        public static void main(String[] args) {
		try { 
			
			//if(true) return;
                        
                         System.out.println("abc");
                        try {

                        String host ="127.0.0.1:63148"; 
                        //String tmpIOR = NotesFactory.getIOR(host); 
                        session = NotesFactory.createSession(host, "Jonatan Raimir Santos da Silva/CONSISTE", "(ca)153");
                        System.out.println(session.getEffectiveUserName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            
                        }
                        //session = getSession();
			XTR xtr = new XTR();
                        dbInstalacao = xtr.getDatabase("xtr-tinto/CONSISTE", "xtr-data\\instalacao-v1.nsf");//bd de instala��es
			
			//print de inicializa��o
			//System.out.println( "Iniciando Agent " + ag.getName() + " as "  + new Date() );
			
			View vInstalacao = null;
			Document docInstalacao = null;
			Document docAuxInst = null;
			
			Database dbFila = null;
			View vFila = null;
			
			if ( dbInstalacao != null ) {
				vInstalacao = dbInstalacao.getView( "INSTALACAO-cod" );
				docInstalacao = vInstalacao.getFirstDocument();
				
				//enviando a fila por instala��o
				while ( docInstalacao != null ) {
					docAuxInst = vInstalacao.getNextDocument( docInstalacao );
					String identificacao = docInstalacao.getItemValueString( "identificacao" );
					
					//pegando os base de dados da fila de email
					dbFila = xtr.getDatabase( "xtr-tinto/CONSISTE", ("app-data\\" + identificacao.toLowerCase() + "\\filaemail-v1.nsf") );
					
					//verificando se o db foi aberto
					if (  dbFila == null ) {
						docInstalacao = docAuxInst;
						continue;
					}
					
					vFila = dbFila.getView( "FILAEMAIL-status" );
					docFila = vFila.getFirstDocument();
					docAuxFila = null;
					
					ExecutorService executor = Executors.newFixedThreadPool(10);
					int count = 0;
					//iniciando a fila
					while ( docFila != null ) {
						
						/*
						if( count == 100 ) {
							docFila = null;
							continue;
						}
						*/
						count++;
						docAuxFila = vFila.getNextDocument( docFila );
						
						//verificando se status do documento da fila � aguardando
						if ( !docFila.getItemValueString( "status" ).toLowerCase().equals("aguardando") ) { 
							//docFila = docAuxFila;
							docFila = null;
							continue;
							//if ( true ) return;
						}	
						
						docFila.replaceItemValue( "status", "EXECUTANDO" );
						
						if ( !docFila.save(false, false) ) {
							docFila = docAuxFila;
							continue; 
						}
						

						if ( identificacao.equals("consiste")) {
							
						}
						
						String xtrID = docFila.getItemValueString("xtr_cod");
						final Runnable worker;
						worker = new Corredor(session, dbFila, xtrID);
			            executor.execute( new Runnable(){
			            	public void run(){
			            		NotesThread th;
			            		try {
				            		th = new NotesThread(worker);
				            		th.start();
									th.join();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
			            	}
			            });
				        
						//reciclando e pegando o pr�ximo documento da fila
						//docFila.replaceItemValue( "status", "XYZ" );
						//docFila.replaceItemValue( "status", "aguardando" );
			            docFila.replaceItemValue( "status", "CONCLUIDO" );
			            docFila.save();
						docFila.recycle();
						docFila = docAuxFila;
					}//fim while docFila
					
					
					//System.out.println("isTerminated: " + executor.isTerminated());
			        executor.shutdown();
			        while (!executor.isTerminated()) {
			        }
			        System.out.println("Finished all threads");
					
					//reciclando objetos notes
					vFila.recycle();
					dbFila.recycle();
					docInstalacao.recycle();
					docInstalacao = docAuxInst;
				}// fim do while docInstalacao
				
				vInstalacao.recycle();
				dbInstalacao.recycle();
			}
		} catch( Exception e) {
			e.printStackTrace();
			System.out.println("Message: " + e.getMessage());
		}
		finally {
			try {
				session.recycle();
			} 
			catch( NotesException e) {}
		}
	}
}