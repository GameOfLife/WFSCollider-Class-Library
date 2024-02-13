/*
\poll_point

An UMap that posts incoming point in the 'post' window. The point itself is passed through unchanged.

point: the point to be posted
trigger: a trigger causes the value to be posted
onChange: when true, the value will be posted whenever it changes (can result in many posts)
speed: number of times per second to post the value automatically (default 0 - no automatic posting).

The value arg range is mapped to that of the parameter to which the UMap is connected.

--
this is an UMapDef definition file
part of the WFSCollider-Class-Library default UMapDefs set
*/

UMapDef( \poll_point, { |point = #[0.0, 0.0]|
	var speed, trigger, change, onChange, mappedVal;
	trigger = \trigger.tr( 1 );
	onChange = \onChange.kr( 0 );
	speed = \speed.kr( 0 );
	change = HPZ1.kr( point.sum ).abs > 0;
	change = (HPZ1.kr( change ).abs > 0) + (TDuty.kr( 0.1, change ) * change);
	change = change * onChange;
	Poll.kr( Impulse.kr( speed ) + trigger + change,
		point,
		"poll_point" );
	UMapOut.kr(point, false);
})
.setSpec( \point, WFSPointSpec(200, 0.1) )
.setSpec( \onChange, BoolSpec( false ) )
.setSpec( \speed, [0,20,\lin,0,0].asSpec )
.setSpec( \trigger, TriggerSpec( ) )
.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})
.category_( 'point_utility' )