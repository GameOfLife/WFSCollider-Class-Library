/*
Things to store in config:
* server ips and ports
* sound card name
* number of output channels
* speaker config
	+ angles, distances
* load all defs at startup, yes, no
* load synthdefs via remote folder:
	+ enabled/disabled
	+ location of folder

VBAPOptions.fromPreset(\soniclab)
*/

VBAPOptions {
	classvar <>presets;
	classvar <>current;

	var <>serverDescs; // Array[Array[ip,port]]
	var <>device;
	var <>numOutputChannels;
	var <>angles;
	var <>distances;
	var <>loadDefsAtStartup = true;
	var <>loadUdefViaRemoteFolder = false;
	var <>remoteFolderForLoading;
	var <>isSlave = false;
	var <>extraDefFolders = #[];


	*initClass {
		presets = (
			\soniclab: VBAPOptions(
				8.collect{ |i| ["slave "++(i+1),"192.168.1.100", 57456 + i] },
				"HDSPe MADI (Slot-2)",
				48,
				[
					[ -25.400967366, 0 ],
					[ 25.9660516833, 0 ],
					[ -67.0576274588, 0 ],
					[ 66.6879780945, 0 ],
					[ -107.4110491464, 0 ],
					[ 107.3832377266, 0 ],
					[ -146.2172414806, 0 ],
					[ 146.1054525865, 0 ],
					[ -20.4495476108, 17.8750032812 ],
					[ 20.571247071, 17.8150833255 ],
					[ -65.6744247609, 24.0287030727 ],
					[ 65.6416496995, 23.9189613576 ],
					[ -108.1204280324, 24.7878302634 ],
					[ 108.1462414292, 24.906009848 ],
					[ -155.454155974, 23.6313179834 ],
					[ 155.6036500106, 23.5605201117 ],
					[ -27.5218854663, 52.7448792263 ],
					[ 28.2311587404, 52.8541958285 ],
					[ -74.594892688, 57.9362142825 ],
					[ 73.451059145, 59.0965878748 ],
					[ -113.1577815608, 57.1078901345 ],
					[ 110.9030667368, 56.4426902381 ],
					[ -151.3671810066, 54.2142657891 ],
					[ 152.046597927, 52.8541958285 ],
					[ -28.0372821602, -50.4062940572 ],
					[ 28.0372821602, -50.4062940572 ],
					[ -77.6309384741, -52.9029151782 ],
					[ 77.1957339347, -53.8362268718 ],
					[ -116.4957697362, -51.38202567 ],
					[ 116.7749248886, -51.6531568981 ],
					[ -159.9660768355, -50.2322714936 ],
					[ 157.4926118991, -49.7580403359 ]
				],
				[
					6.31, 6.34, 6.67, 6.57, 6.45, 6.46, 7.64, 7.65, 9.22,
					9.25, 6.95, 6.98, 6.75, 6.72, 7.06, 7.08, 6.91, 6.9,
					6.49, 6.41, 6.55, 6.6, 6.78, 6.9, 6.0084357365, 6.0084357365,
					5.8048083517, 5.7349280728, 5.9258332747, 5.9035836574,
					6.0235952719, 6.0655832366
				],
				true,
				false,
				"",
				false
			),
			\soniclabTest: VBAPOptions(
				4.collect{ |i| ["slave "++(i+1),"localhost", 57456 + i] },
				nil,
				48,
				[
					[ -25.400967366, 0 ],
					[ 25.9660516833, 0 ],
					[ -67.0576274588, 0 ],
					[ 66.6879780945, 0 ],
					[ -107.4110491464, 0 ],
					[ 107.3832377266, 0 ],
					[ -146.2172414806, 0 ],
					[ 146.1054525865, 0 ],
					[ -20.4495476108, 17.8750032812 ],
					[ 20.571247071, 17.8150833255 ],
					[ -65.6744247609, 24.0287030727 ],
					[ 65.6416496995, 23.9189613576 ],
					[ -108.1204280324, 24.7878302634 ],
					[ 108.1462414292, 24.906009848 ],
					[ -155.454155974, 23.6313179834 ],
					[ 155.6036500106, 23.5605201117 ],
					[ -27.5218854663, 52.7448792263 ],
					[ 28.2311587404, 52.8541958285 ],
					[ -74.594892688, 57.9362142825 ],
					[ 73.451059145, 59.0965878748 ],
					[ -113.1577815608, 57.1078901345 ],
					[ 110.9030667368, 56.4426902381 ],
					[ -151.3671810066, 54.2142657891 ],
					[ 152.046597927, 52.8541958285 ],
					[ -28.0372821602, -50.4062940572 ],
					[ 28.0372821602, -50.4062940572 ],
					[ -77.6309384741, -52.9029151782 ],
					[ 77.1957339347, -53.8362268718 ],
					[ -116.4957697362, -51.38202567 ],
					[ 116.7749248886, -51.6531568981 ],
					[ -159.9660768355, -50.2322714936 ],
					[ 157.4926118991, -49.7580403359 ]
				],
				[
					6.31, 6.34, 6.67, 6.57, 6.45, 6.46, 7.64, 7.65, 9.22,
					9.25, 6.95, 6.98, 6.75, 6.72, 7.06, 7.08, 6.91, 6.9,
					6.49, 6.41, 6.55, 6.6, 6.78, 6.9, 6.0084357365, 6.0084357365,
					5.8048083517, 5.7349280728, 5.9258332747, 5.9035836574,
					6.0235952719, 6.0655832366
				],
				true,
				false,
				"",
				false
			),
			\soniclabSlave: VBAPOptions(
				8.collect{ |i| ["slave "++(i+1),"localhost", 57456 + i] },
				"HDSPe MADI (Slot-2)",
				48,
				nil,
				nil,
				true,
				false,
				"",
				true
			),
			\fivePointOne: VBAPOptions(
				4.collect{ |i| ["slave "++(i+1),"localhost", 57456 + i] },
				nil,
				6,
				[-30, 30, 0, -110, 110],
				nil,
				true,
				false,
				nil,
				false
			),
			\octo: VBAPOptions(
				4.collect{ |i| ["slave "++(i+1),"localhost", 57456 + i] },
				nil,
				8,
				[0, 45, 90, 135, 180, -135, -90, -45],
				nil,
				true,
				false,
				nil,
				false
			),
			\quad: VBAPOptions(
				4.collect{ |i| ["slave "++(i+1),"localhost", 57456 + i] },
				nil,
				8,
				[-45, 45, -135, 135],
				nil,
				true,
				false,
				nil,
				false
			)
		)
	}

	*fromPreset { |key|
		^presets.at(key)
	}

	*new { | serverDescs, device, numOutputChannels = 32, angles, distances, loadDefsAtStartup = true,
		loadUdefViaRemoteFolder = false, remoteFolderForLoading, isSlave = false, extraDefFolders|
		^super.newCopyArgs(serverDescs, device, numOutputChannels, angles, distances, loadDefsAtStartup,
			loadUdefViaRemoteFolder, remoteFolderForLoading, isSlave, extraDefFolders ? [])
	}
}