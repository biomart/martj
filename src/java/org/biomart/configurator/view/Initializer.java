package org.biomart.configurator.view;


public class Initializer extends Root {

	public Initializer() {
		super();
		// TODO Auto-generated constructor stub
		System.out.println("Init. Constructor: yam");		
	}
	
	/**
	 * @param args
	 */
	public int name()
	{
		System.out.println("Init. name: wow, HelloWorld");
		return 1;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Init. Main: yam Again :)");
		Initializer InitObj = new Initializer();
		InitObj.name();
	}
}
