/*
\p_sequencer_point

Creates an pattern UMap for sequencing a group of points. The next point is chosen each time the chain is started, or in a UPattern for each of the generated events.

points: a WFSPointGroup containing the points to sequence
loop: loop mode;
	\off: loop once and stay at last point
	\loop: loop from first to last
	\backwards: loop from last to first
	\alternate: loop forwards and backwards

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UPatDef( \p_sequencer_point, { |unit,
		points,
		loop = \loop|

	{
		var current = 0, direction = 1;
		points.next.wrapAt( current ).yield;
		inf.do { |i|
			var pointsNext, size = 0;
			current = current + direction;
			pointsNext = points.next;
			if( pointsNext.notNil ) {
				size = pointsNext.size;
			};
			switch( loop.next,
				\loop, { current = current.wrap( 0, size -1); direction = 1; },
				\backwards, { current = current.wrap( 0, size -1); direction = -1; },
				\alternate, {
					case { current >= size} {
						current = current.fold(0,size-1);
						direction = -1;
					} { current < 0 } {
						current = current.fold(0,size-1);
						direction = 1;
					};
				},
				\random, {
					current = size.rand;
				},
				\off, { current = current.clip(0,size-1); direction = 1; }
			);
			pointsNext.wrapAt( current ).yield;
		};
	}.r
}).category_( 'pattern_point' )
	.valueIsMapped_( false )
	.numChannels_( 2 )
	.setSpec( \loop, ListSpec( [ \off, \loop, \backwards, \alternate, \random ], 1 ) )
	.setDefault( \value, 0.0@0.0 )
    .setSpec( \points, WFSMultiPointSpec( 200, default: WFSPointGroup.generate( 8, \circle ).positions  ) )
	.setDefault( \points, WFSPointGroup.generate( 8, \circle ).positions )
	.canUseUMapFunc_({ |unit, key, umapdef|
		[ PointSpec, WFSPointSpec, WFSRadiusSpec, WFSPlaneSpec, UAdaptSpec ].any({ |class|
			unit.getSpec( key ).isMemberOf( class )
		});
	});