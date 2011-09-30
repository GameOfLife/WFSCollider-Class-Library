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
		
	}
	
	adjustCorner2To { |aWFSArrayConf|
		aWFSArrayConf = aWFSArrayConf.asWFSArrayConf;
		cornerAngles[1] = (angle - aWFSArrayConf.angle).wrap(-pi,pi).abs;
		corners[1] =  ( dist - ( aWFSArrayConf.dist/ cos(cornerAngles[1]) ) )
			/ tan(cornerAngles[1]);
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
	
	storeArgs { ^arrayConfs.collect(_.storeArgs) }
	
}
