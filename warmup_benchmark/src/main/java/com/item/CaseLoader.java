package com.item;

/**
 * @author: ChangYajie
 * @date: 2019/8/14
 */

import com.item.annotations.Benchmark;
import com.item.annotations.Measurement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * @author: ChangYajie
 * @date: 2019/8/13
 */

class CaseRunner{
    private final List<Case> caseList;
    //默认配置
    private static final int DEFAULT_ITERATION =10;
    private static final int DEFAULT_GROUP = 5;
    private static final int DEFAULT_ITERATIONS_WARMUP = 2;

    public CaseRunner(List<Case> caseList){
        this.caseList = caseList;
    }
    public void run() throws InvocationTargetException, IllegalAccessException {
        for (Case bCase : caseList){
            int iterations = DEFAULT_ITERATION;
            int group = DEFAULT_GROUP;
            int iterationsWarmUp = DEFAULT_ITERATIONS_WARMUP;
            //先获取类级别的配置
            Measurement classMeasurement = bCase.getClass().getAnnotation(Measurement.class);
            if (classMeasurement != null){
                iterations = classMeasurement.iterations();
                group = classMeasurement.group();
            }

            //找到对象中哪些方法是需要测试的：
            Method[] methods = bCase.getClass().getMethods();
            for (Method method : methods){
                Benchmark benchmark = method.getAnnotation(Benchmark.class);
                //如果方法中没有benchmark注解，则不属于要测试的方法，跳过
                if (benchmark == null){
                    continue;
                }

                //针对方法的配置
                Measurement methodMeasurement = method.getAnnotation(Measurement.class);
                if (methodMeasurement != null){
                    iterations = methodMeasurement.iterations();
                    group = methodMeasurement.group();
                }

                runCase(bCase,method,iterationsWarmUp,iterations,group);
            }
        }
    }

    private void runCase(Case bCase, Method method, int iterationsWarmUp, int iterations, int group) throws InvocationTargetException, IllegalAccessException {

        String caseName = bCase.getClass().getName();
        String methodName = method.getName();

        System.out.println(caseName+"的"+methodName+"方法"+" 的测试报告如下：");

        //预热检测部分
        if(iterationsWarmUp != 0) {
            System.out.println("-----------------------------------------------------");
            System.out.println("预热开始..........");
            for(int k=0; k<iterationsWarmUp; k++) {
                System.out.print("第 "+(k+1)+" 次预热：");

                long timeStart = System.nanoTime();

                //每个预热单位默认执行50次
                for(int m=0; m<50;m++) {
                    method.invoke(bCase);
                }

                long timeEnd = System.nanoTime();

                long time = timeEnd - timeStart;
                System.out.println("耗时："+time/50);
            }
            System.out.println("预热完毕..........");
            System.out.println("-----------------------------------------------------");
        }

        //真正的测试，调用对象的测试实例方法
        for (int i = 0;i < group;i++){
            System.out.printf("第 %d 次实验，",i+1);
            long t1 = System.nanoTime();
            for (int j = 0;j < iterations;j++){
                method.invoke(bCase);
            }
            long t2 = System.nanoTime();

            System.out.printf("耗时 %d 纳秒 %n",t2 - t1);
        }
    }
}



public class CaseLoader {
    public CaseRunner load() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String pkgDot = "com.item.cases";
        String pkg = "com/item/cases";
        List<String> classNameList = new ArrayList<String>();
        //1.根据一个固定的类找到类加载器
        //2.根据类加载器找到类文件所在的路径
        //3.扫描路径的所有类文件
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> urls = classLoader.getResources(pkg);

        while(urls.hasMoreElements()){
            URL url = urls.nextElement();
            if (!url.getProtocol().equals("file")){
                //如果不是*.class文件，暂不支持
                continue;
            }
            String dirname = URLDecoder.decode(url.getPath(),"UTF-8");
            File dir = new File(dirname);
            if (!dir.isDirectory()){
                //不是目录
                continue;
            }
            File[] files = dir.listFiles();
            if (files == null){
                continue;
            }
            for (File file : files){
                //
                String filename = file.getName();
                String className = filename.substring(0,filename.length()-6);
                classNameList.add(className);
            }
        }

        List<Case> caseList = new ArrayList<Case>();
        for (String className : classNameList){
            Class<?> cls = Class.forName(pkgDot+"."+className);
            if (hasInterface(cls,Case.class)){
                caseList.add((Case)cls.newInstance());
            }

        }

        return new CaseRunner(caseList);
    }

    private boolean hasInterface(Class<?> cls,Class<?> intf){
        Class<?>[] intfs = cls.getInterfaces();
        for (Class<?> i : intfs){
            if (i == intf){
                return true;
            }
        }
        return false;
    }
}

