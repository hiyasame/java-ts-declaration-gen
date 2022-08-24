# java-ts-declaration-gen

> 通过解析jar文件中的类文件的字节码生成用于java与js之间交互补全的d.ts声明文件

## usage

> java -jar javatypes-dts-gen.jar <目标jar文件绝对路径>

会在jar文件所在目录生成一个npm项目，项目根目录下dist文件夹中有所有的d.ts文件，其中放有所有d.ts文件。建议发布直接发布npm包，使用时在项目中引用即可。

## 效果

原java类, 省略方法内容 (代码来源: 反编译) 

~~~java
package org.mozilla.classfile;

import org.mozilla.classfile.ClassFileWriter;

final class ClassFileField {
    private short itsNameIndex;
    private short itsTypeIndex;
    private short itsFlags;
    private boolean itsHasAttributes;
    private short itsAttr1;
    private short itsAttr2;
    private short itsAttr3;
    private int itsIndex;

    ClassFileField(short nameIndex, short typeIndex, short flags);

    void setAttributes(short attr1, short attr2, short attr3, int index);

    int write(byte[] data, int offset);

    int getWriteSize();
}
~~~

生成的d.ts文件

~~~ts
declare namespace org {
    namespace mozilla {
        namespace classfile {
            // @ts-ignore
            class ClassFileField extends java.lang.Object {
                // @ts-ignore
                constructor(param0: number /** S **/, param1: number /** S **/, param2: number /** S **/)
                // @ts-ignore
                setAttributes(param0: number /** S **/, param1: number /** S **/, param2: number /** S **/, param3: number /** I **/): void
                // @ts-ignore
                write(param0: Array<number /** B **/>, param1: number /** I **/): number /** I **/
                // @ts-ignore
                getWriteSize(): number /** I **/
                // @ts-ignore
                private itsNameIndex: number /** S **/
                // @ts-ignore
                private itsTypeIndex: number /** S **/
                // @ts-ignore
                private itsFlags: number /** S **/
                // @ts-ignore
                private itsHasAttributes: boolean
                // @ts-ignore
                private itsAttr1: number /** S **/
                // @ts-ignore
                private itsAttr2: number /** S **/
                // @ts-ignore
                private itsAttr3: number /** S **/
                // @ts-ignore
                private itsIndex: number /** I **/
            }
        }
    }
}
~~~

## TODO

- [x] 内部类逻辑处理
- [x] 过滤掉Kotlin挂起函数 & 匿名类
- [ ] 将函数式接口转换为ts function类型