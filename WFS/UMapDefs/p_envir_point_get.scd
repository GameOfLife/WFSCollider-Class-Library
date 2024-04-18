/*
\p_envir_point_get

Creates an pattern UMap for getting environment variables. Use in conjunction with the 'envir_set' Udef. Environment values are also accessible via regular SuperCollider code, by adding a '~' in front of the name.

key: the name of the variable (default: 'a')

The value will automatically be scaled to the value range of the parameter to which the UMap is connected.

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UPatDef( \p_envir_point_get, { |unit, key = 'p', index = 0|
	{
		var kkey, val, spec, fromSpec, ii = 0;
		spec = unit.spec ?? { WFSPointSpec(200) };
		loop {
			kkey = key.next.asSymbol;
			if( kkey.notNil ) {
				val = currentEnvironment[ kkey ] ? (spec.default);
				fromSpec = currentEnvironment[ \u_specs ] !? _[ kkey ] ? spec;
				if( val.isCollection ) {
					val = val.wrapAt( ii );
					ii = ii+1;
				};
				if( val.isKindOf( Point ).not ) {
					val = spec.constrain( val.asPoint );
				};
			} {
				val = spec.default;
			};
			val.yield;
		};
	}.r
})
	.setSpec( \key, SymbolSpec() )
    .setSpec( \index, IntegerSpec(0,0,1024) )
	.numChannels_( 2 )
	.valueIsMapped_( false )
	.mappedArgs_([])
	.canUseUMapFunc_({ |unit, key, umapdef|
		[ PointSpec, WFSPointSpec, WFSRadiusSpec, WFSPlaneSpec, UAdaptSpec ].any({ |class|
			unit.getSpec( key ).isMemberOf( class )
		});
	})
	.category_( 'pattern_envir' );