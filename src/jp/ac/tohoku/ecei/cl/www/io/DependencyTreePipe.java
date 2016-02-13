package jp.ac.tohoku.ecei.cl.www.io;

import jp.ac.tohoku.ecei.cl.www.base.*;

public interface DependencyTreePipe {

    public DependencyTree[] pipe();
    public DependencyTree partialPipe();
    public boolean eof();
}
