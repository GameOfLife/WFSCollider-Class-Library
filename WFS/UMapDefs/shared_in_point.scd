/*
\shared_in_point

** this UMapDef should be used in conjunction with 'shared_out_point' **

The shared_in_point UMapDef can receive point information from UMaps used earlier in the chain. This is useful when multiple units or unit parameters need to use the same point information, or derrive things from it. The shared_in_point would always need to come after a 'shared_out_point' in the order of the chain; it recieves the point data from a bus. To send the data in an other unit or parameter use the 'shared_out_point' UMapDef, and make sure the 'id' setting is the same on both. There can be multiple 'shared_in_point' UMaps with the same 'id' in one chain, which would all receive data from the same 'shared_out_point'. All this will only work _within_ a single chain.

id: the id (0-99) by which the point can be retreived from 'shared_out_point'

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UMapDef( \shared_in_point, { |id = 0|
	var sig;
	sig = In.kr((id * 2) + [1200,1201]);
	UMapOut.kr(sig, false);
})
	.setSpec( \id, IntegerSpec( 0, 0, 99 ) )
	.category_( 'point_utility' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	});