package org.opengeo.data.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Directory extends FileData {

    /**
     * list of files contained in directory
     */
    List<FileData> files = new ArrayList<FileData>();
    
    public Directory(File file) {
        super(file);
    }

    public File getFile() {
        return file;
    }

    public List<FileData> getFiles() {
        return files;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public void prepare() throws IOException {
        files = new ArrayList<FileData>();

        //recursively search for spatial files, maintain a queue of directories to recurse into
        LinkedList<File> q = new LinkedList<File>();
        q.add(file);

        while(!q.isEmpty()) {
            File dir = q.poll();

            //get all the regular (non directory) files
            Set<File> all = new LinkedHashSet<File>(Arrays.asList(dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return !new File(dir, name).isDirectory();
                }
            })));

            //scan all the files looking for spatial ones
            for (File f : dir.listFiles()) {
                if (f.isHidden()) {
                    continue;
                }
                if (f.isDirectory()) {
                    Directory d = new Directory(f);
                    d.prepare();
                    
                    files.add(d);
                    //q.push(f);
                    continue;
                }

                //determine if this is a spatial format or not
                DataFormat format = DataFormat.lookup(f);

                if (format != null) {
                    SpatialFile sf = new SpatialFile(f);
                    sf.setFormat(format);

                    //gather up the related files
                    sf.prepare();

                    files.add(sf);

                    all.removeAll(sf.allFiles());
                }
            }

            //take any left overs and add them as unspatial/unrecognized
            for (File f : all) {
                files.add(new ASpatialFile(f));
            }
        }

        format = format();
//        //process ignored for files that should be grouped with the spatial files
//        for (DataFile df : files) {
//            SpatialFile sf = (SpatialFile) df;
//            String base = FilenameUtils.getBaseName(sf.getFile().getName());
//            for (Iterator<File> i = ignored.iterator(); i.hasNext(); ) {
//                File f = i.next();
//                if (base.equals(FilenameUtils.getBaseName(f.getName()))) {
//                    //.prj file?
//                    if ("prj".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()))) {
//                        sf.setPrjFile(f);
//                    }
//                    else {
//                        sf.getSuppFiles().add(f);
//                    }
//                    i.remove();
//                }
//            }
//        }
//        
//        //take any left overs and add them as unspatial/unrecognized
//        for (File f : ignored) {
//            files.add(new ASpatialFile(f));
//        }
//        
//        return files;
//        
//        for (DataFile f : files()) {
//            f.prepare();
//        }
    }

    public List<Directory> flatten() {
        List<Directory> flat = new ArrayList<Directory>();

        LinkedList<Directory> q = new LinkedList<Directory>();
        q.push(this);
        while(!q.isEmpty()) {
            Directory dir = q.pop();
            flat.add(dir);

            for (Iterator<FileData> it = dir.getFiles().iterator(); it.hasNext(); ) {
                FileData f = it.next();
                if (f instanceof Directory) {
                    Directory d = (Directory) f;
                    it.remove();
                    q.push(d);
                }
            }
        }

        return flat;
    }

//    public List<DataFile> files() throws IOException {
//        LinkedList<DataFile> files = new LinkedList<DataFile>();
//        
//        LinkedList<File> ignored = new LinkedList<File>();
//
//        LinkedList<File> q = new LinkedList<File>();
//        q.add(file);
//
//        while(!q.isEmpty()) {
//            File f = q.poll();
//
//            if (f.isDirectory()) {
//                q.addAll(Arrays.asList(f.listFiles()));
//                continue;
//            }
//
//            //determine if this is a spatial format or not
//            DataFormat format = DataFormat.lookup(f);
//
//            if (format != null) {
//                SpatialFile file = new SpatialFile(f);
//                file.setFormat(format);
//                files.add(file);
//            }
//            else {
//                ignored.add(f);
//            }
//        }
//        
//        //process ignored for files that should be grouped with the spatial files
//        for (DataFile df : files) {
//            SpatialFile sf = (SpatialFile) df;
//            String base = FilenameUtils.getBaseName(sf.getFile().getName());
//            for (Iterator<File> i = ignored.iterator(); i.hasNext(); ) {
//                File f = i.next();
//                if (base.equals(FilenameUtils.getBaseName(f.getName()))) {
//                    //.prj file?
//                    if ("prj".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()))) {
//                        sf.setPrjFile(f);
//                    }
//                    else {
//                        sf.getSuppFiles().add(f);
//                    }
//                    i.remove();
//                }
//            }
//        }
//        
//        //take any left overs and add them as unspatial/unrecognized
//        for (File f : ignored) {
//            files.add(new ASpatialFile(f));
//        }
//        
//        return files;
//    }

    /**
     * Returns the data format of the files in the directory iff all the files are of the same 
     * format, if they are not this returns null.
     */
    public DataFormat format() throws IOException {
        if (files.isEmpty()) {
            return null;
        }

        FileData file = files.get(0);
        DataFormat format = file.getFormat();
        for (int i = 1; i < files.size(); i++) {
            FileData other = files.get(i);
            if (format != null && !format.equals(other.getFormat())) {
                return null;
            }
            if (format == null && other.getFormat() != null) {
                return null;
            }
        }

        return format;
    }

    public Directory filter(List<FileData> files) {
        return new Filtered(file, files);
    }

    @Override
    public String toString() {
        return file.getPath();
    }

    static class Filtered extends Directory {

        List<FileData> filter;

        public Filtered(File file, List<FileData> filter) {
            super(file);
            this.filter = filter;
        }

        @Override
        public void prepare() throws IOException {
            super.prepare();

            files.retainAll(filter);
            format = format();
        }
    }
}