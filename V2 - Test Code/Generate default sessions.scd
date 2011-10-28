

// 8 live sources with path player
USession(*8.collect{ |x| 
	var c = UChain(\wfsMasterIn, \wfsPathPlayer, \wfsDynamicPoint); c.units[0].set(\bus,x); c }).gui
	
//8 live sources no path player
USession(*8.collect{ |x| 
	var c = UChain(\wfsMasterIn, \wfsPathPlayer, \wfsDynamicPoint); c.units[0].set(\bus,x); c }).gui

