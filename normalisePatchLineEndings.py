import sys, os
    
def normalisePatchLineEndings():

    base_dir = os.path.dirname(os.path.abspath(__file__))
    patchDir = os.path.join(base_dir, 'patches')
    for root, dirs, files in os.walk(patchDir):
        for file in files:
            if file.endswith(".java.patch"):
                fileContent = ''
                patchFile = os.path.join(root, file)
                with open( patchFile, 'rb') as input:
                    fileContent = input.read()
                with open( patchFile, 'wb') as out:
                    out.write( fileContent.replace('\r\n','\n').replace('\r','\n') )
            
if __name__ == '__main__':
    normalisePatchLineEndings()
