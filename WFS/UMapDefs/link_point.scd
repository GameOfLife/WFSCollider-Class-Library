FuncUMapDef( \link_point, { |unit, u_store, url|
	u_store.spec = unit.spec;
	u_store.url = url;
	u_store;
}, [ [ \u_store, ULink(), ULinkSpec(), true ], [ \url, "", StringSpec("") ] ], valueIsPrivate: true )
	.valueIsMapped_( false )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})
	.dontStoreValue_(true)
	.numChannels_(2)
	.setSpec( \value, ULinkSpec() )
	.category_( 'point' )