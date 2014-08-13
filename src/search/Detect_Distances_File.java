package search;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;

public class Detect_Distances_File implements PlugIn
{
	public static String outputfilePTM = "1_6.points_to_mask.loc";
	public static String outputfileMTP = "1_6.mask_to_points.loc";
	public static String textfile = "1_6.loc";
	public static String imagefileCoordinates = "Results.txt";

	@Override
	public void run( String arg0 )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Input" );
		
		gd.addFileField( "Segmented Image Particles", imagefileCoordinates, 50 );
		gd.addFileField( "RNA's (textfile, multicolumn)", textfile, 50 );
		gd.addFileField( "Outputfile (points to mask)", outputfilePTM, 50 );
		gd.addFileField( "Outputfile (mask to points)", outputfileMTP, 50 );
		
		gd.showDialog();

		if ( gd.wasCanceled() )
			return;
		
		imagefileCoordinates = gd.getNextString();
		textfile = gd.getNextString();
		outputfilePTM = gd.getNextString();
		outputfileMTP = gd.getNextString();

		if ( !new File( imagefileCoordinates ).exists() )
		{
			IJ.log( "File not found: " + imagefileCoordinates );
			return;
		}

		if ( !new File( textfile ).exists() )
		{
			IJ.log( "TextFile not found: " + textfile );
			return;
		}

		try 
		{
			final double[] tmp = new double[ 2 ];

			final BufferedReader in1 = TextFileAccess.openFileRead( imagefileCoordinates );
			final ArrayList< RealPoint > synapses = new ArrayList<RealPoint>();
			
			String[] header = in1.readLine().trim().split( "\t" );
			
			int indexXM = -1;
			int indexYM = -1;
			
			for ( int i = 0; i < header.length; ++i )
			{
				if ( header[ i ].toLowerCase().equals( "xm" ) )
					indexXM = i + 1;

				if ( header[ i ].toLowerCase().equals( "ym" ) )
					indexYM = i + 1;
			}

			if ( indexXM == -1 )
			{
				IJ.log( "Could not find column named 'XM'" );
				return;
			}
			
			if ( indexYM == -1 )
			{
				IJ.log( "Could not find column named 'YM'" );
				return;
			}

			while ( in1.ready() )
			{
				final String[] line = in1.readLine().trim().split( "\t" );
				
				tmp[ 0 ] = Double.parseDouble( line[ indexXM ] );
				tmp[ 1 ] = Double.parseDouble( line[ indexYM ] );

				synapses.add( new RealPoint( tmp[ 0 ], tmp[ 1 ] ) );
			}
			
			IJ.log( "Read " + synapses.size() + " synapse particles." );
			
			in1.close();
			
			final BufferedReader in = TextFileAccess.openFileRead( textfile );
			final ArrayList< RealPoint > spots = new ArrayList<RealPoint>();

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

				spots.add( new RealPoint( tmp[ 0 ], tmp[ 1 ] ) );
			}
			
			in.close();

			IJ.log( "Read " + spots.size() + " smFISH spots." );
			
			final KDTree< RealPoint > kdTreeMask = new KDTree< RealPoint >( synapses, synapses );
			final NearestNeighborSearchOnKDTree< RealPoint > searchMask = new NearestNeighborSearchOnKDTree<RealPoint>( kdTreeMask );

			// write out result	outputfilePTM
			PrintWriter out = TextFileAccess.openFileWrite( outputfilePTM );

			for ( final RealPoint point : spots )
			{
				searchMask.search( point );
				final double distance = searchMask.getDistance();

				out.println( (point.getDoublePosition( 0 ) + 0.5) + "\t" + (point.getDoublePosition( 1 ) + 0.5) + "\t" + distance );
			}
			
			out.close();

			final KDTree< RealPoint > kdTreeSpots = new KDTree< RealPoint >( spots, spots );
			final NearestNeighborSearchOnKDTree< RealPoint > searchSpots = new NearestNeighborSearchOnKDTree<RealPoint>( kdTreeSpots );

			// write out result	outputfilePTM
			out = TextFileAccess.openFileWrite( outputfileMTP );

			for ( final RealPoint maskPoint : synapses )
			{
				searchSpots.search( maskPoint );
				final double distance = searchSpots.getDistance();

				out.println( (maskPoint.getDoublePosition( 0 )) + "\t" + (maskPoint.getDoublePosition( 1 )) + "\t" + distance );
			}
			
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
		new Detect_Distances_File().run( null );
	}

}