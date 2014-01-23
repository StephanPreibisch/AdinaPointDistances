package search;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RealPoint;
import net.imglib2.collection.KDTree;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;

public class Detect_Distances implements PlugIn
{
	public static String outputfile = "1_6.new.loc";
	public static String textfile = "1_6.loc";
	public static String imagefile = "C1-1_6-mask.tif";

	@Override
	public void run( String arg0 )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Input" );
		
		gd.addFileField( "Segmented Image (bg = 0, fg > 1)", imagefile, 50 );
		gd.addFileField( "RNA's (textfile, multicolumn)", textfile, 50 );
		gd.addFileField( "Outputfile", outputfile, 50 );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		imagefile = gd.getNextString();
		textfile = gd.getNextString();
		outputfile = gd.getNextString();

		if ( !new File( imagefile ).exists() )
		{
			IJ.log( "File not found: " + imagefile );
			return;
		}

		if ( !new File( textfile ).exists() )
		{
			IJ.log( "TextFile not found: " + textfile );
			return;
		}
		
		final Img< FloatType > img = ImagePlusAdapter.convertFloat( new ImagePlus( imagefile ) );
		
		final ArrayList< RealPoint > pixelsBigger0 = new ArrayList<RealPoint>();
		
		final Cursor< FloatType > cursor = img.localizingCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			if ( cursor.get().get() > 0 )
				pixelsBigger0.add( new RealPoint( cursor ) );
		}
		
		final KDTree< RealPoint > kdTree = new KDTree< RealPoint >( pixelsBigger0, pixelsBigger0 );
		final NearestNeighborSearchOnKDTree< RealPoint > search = new NearestNeighborSearchOnKDTree<RealPoint>( kdTree );
		
		final BufferedReader in = TextFileAccess.openFileRead( textfile );
		final PrintWriter out = TextFileAccess.openFileWrite( outputfile );
		
		try 
		{
			final double[] tmp = new double[ 2 ];
			
			while ( in.ready() )
			{
				String line = in.readLine().trim();
				
				if ( line.length() < 5 )
					continue;
				
				while ( line.contains( "  ") )
					line = line.replace( "  ", " " );
				
				while ( line.contains( " " ) )
					line = line.replace( " ", "\t" );
				
				String[] entries = line.split( "\t" );
				
				tmp[ 0 ] = Double.parseDouble( entries[ 0 ] ) - 0.5;
				tmp[ 1 ] = Double.parseDouble( entries[ 1 ] ) - 0.5;
				
				final RealPoint p = new RealPoint( tmp );
				search.search( p );
				final double distance = search.getDistance();
				
				out.println( entries[ 0 ] + "\t" + entries[ 1 ] + "\t" + distance );
			}
			
			in.close();
			out.close();
			
			IJ.log( "output written." );
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main ( String[] args )
	{
		new ImageJ();
		new Detect_Distances().run( null );
	}

}