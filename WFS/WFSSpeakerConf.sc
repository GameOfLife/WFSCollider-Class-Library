/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSArrayConf { // configuration for one single speaker array
	
	var <>n = 48, <>dist = 5, <>angle = 0.5pi, <>offset = 0, <>spWidth;
	var <>corners;
	var <>cornerAngles; // angle to next array
	
	/*
	
	A WFSArrayConf describes a single straight line of n equally spaced loudspeakers.
	
		n: number of speakers
		dist (m): distance from center
		angle (radians): angle from center (facing the speakers)
		offset (m): amount of shift to right when facing the speakers
		spWidth (m): spacing between individual speakers
	
	explanation of variables:
	
	angles are counter-clockwise starting at x axis:
	
	 angle 0.5pi: front array      angle 0: righthand side array
	       
	          |y(+)                               |y(+)
	          |                                   |
	          |                                   |        
	       ---+---                                |    
	x(-)      |dist   x(+)              x(-)      |   |   x(+)
	----------+----------               ----------+---+------            
	          |                                   |   | 
	          |                                   |    
	          |                                   |     
	          |y(-)                               |y(-)          
	          
	and so on:
	 	angle -0.5pi: back array
		angle pi (or -pi): lefthand side array
		
		
	
	corners are the points where two adjecent arrays cross:
	
	  array
	---------  x <- corner
	        
	           |a
	           |r
	           |r
	           |a
	           |y
	           
	           
	 each array has two corner points:
	  
	 x  -------------  x
	 1      array      2
	 
	 the cornerAngles are the angles between the adjecent arrays. They are also the area
	 where the crosfade happens when a point passes from behind one array to behind another.
	 
	 
	           |
	array      | angle, crossfade area  <- (0.5pi in this case)
	---------  x ----
	        
	           |a
	           |r
	           |r
	           |a
	           |y
	*/
	
	*new { |n = 48, dist = 5, angle = 0.5pi, offset = 0, spWidth|
		^super.newCopyArgs( n, dist, angle, offset, spWidth ? WFSBasicPan.defaultSpWidth )
			.init;
	}
	
	init {
		corners = [ dist, dist.neg ]; // assumes square setup
		cornerAngles = [ 0.5pi, 0.5pi ]; // assumes rectangular setup
	}
	
	asWFSArrayConf { ^this }
	
	adjustCorner1To { |aWFSArrayConf|
		aWFSArrayConf = aWFSArrayConf.asWFSArrayConf;
		cornerAngles[0] = (angle - aWFSArrayConf.angle).wrap(-pi,pi).abs;
		corners[0] = ( dist - ( aWFSArrayConf.dist/ cos(cornerAngles[0]) ) )
			/ tan(cornerAngles[0]).neg;
		if( corners[0].isNaN ) { corners[0] = 200 };
		
	}
	
	adjustCorner2To { |aWFSArrayConf|
		aWFSArrayConf = aWFSArrayConf.asWFSArrayConf;
		cornerAngles[1] = (angle - aWFSArrayConf.angle).wrap(-pi,pi).abs;
		corners[1] =  ( dist - ( aWFSArrayConf.dist/ cos(cornerAngles[1]) ) )
			/ tan(cornerAngles[1]);
		if( corners[1].isNaN ) { corners[1] = -200 };
	}
	
	
	asArray { ^[ n, dist, angle, offset, spWidth ] }
	asCornersArray { ^(corners ++ cornerAngles); }
	
	asControlInput { ^this.asArray }
	asOSCArgEmbeddedArray { | array| ^this.asArray.asOSCArgEmbeddedArray(array) }
	
	*fromArray { |array| ^this.new( *array ); }
	
	fromCornersArray { |array| // adjust corners / angles from array
		if( array.notNil ) {
			corners = array[[0,1]];
			cornerAngles = array[[2,3]];
		};
	}
	
	pointAt { |index = 0|
		^( dist @ ( ( (index - ((n-1)/2)) * spWidth) - offset ) ).rotate( angle )
	}
	
	firstPoint {
		^this.pointAt(0);
	}
	
	lastPoint {
		^this.pointAt(n-1);
	}
	
	centerPoint {
		^( dist @ offset.neg ).rotate( angle );
	}
		
	asPoints { // for plotting
		^n.collect({ |i| this.pointAt(i)});
	}
	asLine { // for plotting; start point and end point
		^[ this.firstPoint, this.lastPoint ];
	}
	
	cornerPoints {
		^corners.collect({ |c|
			( dist @ c ).rotate( angle );
		});
	}
	
	cornerPoints_ { |array|
		var current;
		array = array.asCollection.extend( 2, nil );
		if( array.any(_.isNil) ) {
			current = this.cornerPoints;
			array[0] = array[0] ? current[0];
			array[1] = array[1] ? current[1];
		};
		this.prCornerPoints_( array );
	}
	
	prCornerPoints_ { |array|
		// re-calculate angle and dist from corner points
		angle = ((array[1] - array[0]).angle + 0.5pi).wrap(-pi,pi);
		dist = array[0].dist(0@0) * (angle - array[0].angle.wrap(-pi,pi)).cos;
		if( dist < 0 ) { 
			angle = (angle + pi).wrap(-pi,pi);
			dist = dist.neg;
			array = array.reverse;
		};
		corners = array.collect({ |pt| pt.rotate( angle.neg ).y });
	}
	
	draw { |mode = \lines| // 1m = 1px
		Pen.use({
			Pen.scale(1,-1);
			switch( mode,
				\lines, { 
					Pen.line( *this.asLine ).stroke 
				},
				\points, {
					this.asPoints.do({ |pt| 
						Pen.addWedge( pt, spWidth / 2, angle - 0.5pi, pi ).fill;
					}) 
			 	}
			);
		});
	}
	
	storeArgs { ^[n, dist, Angle(angle), offset, spWidth] }

}


WFSSpeakerConf {
	
	// a collection of WFSArrayConfs, describing a full setup
	// WFSSpeakerConfs are designed to be fully surrounding setups
	
	classvar <numSystems, <>serverGroups;
	classvar <>default;
	
	var <>arrayConfs;
	var <>arrayLimit = 1;
	
	*new { |...args|
		^super.newCopyArgs().arrayConfs_( args.collect(_.asWFSArrayConf) ).init;
	}
	
	init {
		// adjust corners and cornerAngles to each other
		arrayConfs.do({ |conf, i|
			conf.adjustCorner1To( arrayConfs.wrapAt( i-1 ) );
			conf.adjustCorner2To( arrayConfs.wrapAt( i+1 ) );
		});
	}
	
	// fast creation
	*rect { |nx = 48, ny, dx = 5, dy| // dx/dy: radius (i.e. from center to array)
		ny = ny ? nx;
		dy = dy ? dx;
		^this.new( [ nx, dx, 0.5pi ], [ ny, dy, 0 ], [ nx, dx, -0.5pi ], [ ny, dy, pi ] );
	}
	
	*polygon { |n = 6, r = 5, nsp = 192|
		^this.new( *n.collect({ |i|
			[ (nsp / n).asInt, r, i.linlin(0, n, 0.5pi, -1.5pi) ]
		}) );
	}
	
	makeDefault { default = this; }
	
	at { |index| ^arrayConfs[ index ] }
	
	speakerCount { ^arrayConfs.collect(_.n).sum; }
	
	divideArrays { |n| // split the arrayConfs into n equal (or not so equal) groups
		var division, counter = 0, result = [];
		n = n ? numSystems;
		division = this.speakerCount / n;
		n.do({ |i|
			result = result ++ [ [ ] ];
			while { (result[i].collect(_.n).sum < division) && { counter < arrayConfs.size } } {
				result[i] = result[i] ++ arrayConfs[ counter ];
				counter = counter + 1;
			};
		});
		^result;
	}
	
	getArrays { |i = 0, n| // arrays for single server (server i out of n)
		^this.divideArrays(n)[i];
	} 
	
	getArraysFor { |server|
		var i;
		i = this.class.systemOfServer( server );
		if( i.notNil ) {
			^this.divideArrays[i]
		} {
			^[]; // empty array if not found
		};
	}
	
	firstSpeakerIndexOf { |server|
		var i;
		i = this.class.systemOfServer( server );
		if( i.notNil ) {
			^this.divideArrays[..i-1].flatten(1).collect(_.n).sum;
		} {
			^nil; // nil if not found
		};
	}
	
	lastSpeakerIndexOf { |server|
		var i;
		i = this.class.systemOfServer( server );
		if( i.notNil ) {
			^this.divideArrays[..i].flatten(1).collect(_.n).sum - 1;
		} {
			^nil; // nil if not found
		};
	}
	
	// Server management
	
	*numSystems_ { |n = 2| // number of systems to divide the speakerarrays over
		numSystems = n;
		serverGroups = numSystems.collect({ |i|
			(serverGroups ? [])[i] ?? { Set() };
		});
	}
	
	*addServer { |server, system = 0|
		server = server.asCollection.collect(_.asTarget).collect(_.server);
		server.do({ |server|
			serverGroups[ system ].add( server );
		});
	}
	
	*removeServer { |server|
		server = server.asCollection.collect(_.asTarget).collect(_.server);
		server.do({ |server|
			serverGroups.do(_.remove(server));
		});
	}
	
	*systemOfServer { |server|
		serverGroups.do({ |item, i|
			if( item.includes(server) ) { ^i };
		});
		^nil;
	}
	
	*includesServer { |server|
		^this.systemOfServer( server ).notNil;
	}
	
	*initClass { 
		this.numSystems = 2; // create server library
	}
	
	
	
	// drawing
	asPoints { ^arrayConfs.collect(_.asPoints).flat }
	
	asLines { ^arrayConfs.collect(_.asLine) }
	
	draw { |mode = \lines| arrayConfs.do(_.draw(mode)); }
	
	plot {
	}
	
	storeArgs { ^arrayConfs.collect(_.storeArgs) }
	storeModifiersOn { |stream|
		if( arrayLimit != 1 ) {
			stream << ".arrayLimit_( " << arrayLimit << " )";
		};
	}
}
