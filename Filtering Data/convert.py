import sys, getopt
import csv

def makeSortable( num ):
	if len(num) == 1:
		return '0' + num
	else:
		return num

def dateToNum(date):
	date = date.translate(None,'-')
	return int(date)

def main(argv):
	try:
		opts, args = getopt.getopt(argv,"i:t:b:a:",["inputfile=","type=","beforedate=","afterdate="])
	except getopt.GetoptError:
		print 'convert.py -i --inputfile <filename> -t --type <type,othertype> -b --beforedate <yyyy-mm-dd> -a --afterdate <yyyy-mm-dd>'
		sys.exit(2)

	inputfile = 'example.csv'
	for opt, arg in opts:
		if opt == '--inputfile' or opt == '-i':
			inputfile = arg
	with open(inputfile) as csvfile:
		with open('extras.txt', 'w') as g:
			reader = csv.reader(csvfile)
			#typedict = {'VEHICLE BREAK-IN/THEFT' : '0'}
			typedict = {}
			output = {}
			firstRow = True
			g.write('This is *all* of the data read by the code. Notice that some have been added and rearranged.\n\n')
			g.write('x, y, date, time, typeNum, lat, lon\n')

			typeCheck = False
			typeFilter = []
			bdateCheck = False
			beforeDate = 0
			adateCheck = False
			afterDate = 0
			for opt, arg in opts:
				if opt == '--type' or opt == '-t':
					typeCheck = True
					typeFilter = arg.split(',')
				if opt == '--beforedate' or opt == '-b':
					bdateCheck = True
					beforeDate = dateToNum(arg)
				if opt == '--afterdate' or opt == '-a':
					adateCheck = True
					afterDate = dateToNum(arg)
			w = 80
			h = 140
			#output = [[0 for x in range(w)] for y in range(h)] 
			width = 0.01	#of a grid square
			height = 0.01	#of a grid square
			bottom = 32.5
			left = -117.4
		
			numerrors = 0
			for row in reader:
				if firstRow:
					firstRow = False
					continue
				columnNum = 0
				date = row[1]
				time = row[7]
				type = row[10]
				lat = row[21]
				lon = row[22]
				dateNum = dateToNum(date)
				x = int((float(lat) - bottom) / width)
				if x < 0 or x >= w:
					numerrors += 1
					#continue
				y = int((float(lon) - left) / height)
				if y < 0 or y >= h:
					numerrors += 1
					#continue
				if not type in typedict:
					typedict[type] = str(len(typedict))
				typeNum = makeSortable(typedict[type])
				coordinate = lat + ', ' + lon
				#----FILTERING BELOW----
				if typeCheck and not type in typeFilter:
					continue
				if bdateCheck and dateNum > beforeDate:
					continue
				if adateCheck and dateNum < afterDate:
					continue
				#----DONE FILTERING----
				if not (coordinate in output):
					output[coordinate] = 0
				output[coordinate] += 1
				g.write(str(x) + ', ' + str(y) + ', ' + date + ', ' + time + ', ' + typeNum + ', ' + lat + ', ' + lon + '\n')
			g.write('\nnum errors ' + str(numerrors))
		#for row in output:
		#	textout = ''
		#	for index in range(-1, len(row) - 2):
		#		textout += str(row[index]) + ', '
		#	textout += str(row[len(row) - 1])
		#	print textout
		print 'lat, lon: numberOfCrimes'
		for key in output:
			print key + ': ' + str(output[key])
		typelist = []
		for types,num in typedict.items():
                	typelist.append(makeSortable(num) + ': ' + types)
		with open('types.txt', 'w') as f:
			for row in sorted(typelist):
				f.write(row + '\n') 
if __name__ == "__main__": main(sys.argv[1:])	

