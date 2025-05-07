package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    //根节点
    private TrieNode rootNode = new TrieNode();
    //替换符
    private static final String REPLACEMENT = "***";

    //初始化方法
    @PostConstruct
    public void init(){
        try(
                //读取文件
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(("sensitive-words.txt"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ){
            String keyword;
            //读取文件
            while((keyword = reader.readLine()) != null){
                this.addKeyword(keyword);
            }
        }
        catch(IOException e){
            logger.error("加载敏感词文件失败:" + e.getMessage());
        }
    }
    private void addKeyword(String keyword){
        TrieNode tempNote = rootNode;
        for(int i = 0;i < keyword.length(); i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNote.getSubNode(c);

            if(subNode == null){
                subNode = new TrieNode();
                tempNote.addSubNode(c, subNode);
            }
            tempNote = subNode;
            if(i == keyword.length() - 1){
                tempNote.setKeywordEnd(true);
            }
        }
    }
    //过滤敏感词
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针1
        TrieNode tempNode = rootNode;
        //指针2
        int begin = 0;
        //指针3
        int position = 0;
        //结果
        StringBuilder sb = new StringBuilder();
        while(position < text.length()){
            char c = text.charAt(position);
            //跳过符号
            if(isSymbol(c)){
                if(tempNode == rootNode){
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;
            }
            //检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                //进入下一个位置
                position = ++begin;
                //重新指向根节点
                tempNode = rootNode;
            }
            else if(tempNode.isKeywordEnd()){
                //发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin = ++position;
                //重新指向根节点
                tempNode = rootNode;
            }
            else{
                //检查下一个字符
                position++;
            }
        }
        sb.append(text.substring(begin));
        return sb.toString();
    }
    //判断是否是符号
    private boolean isSymbol(Character c){
        //0x2E80~0x9FFF是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }


    //前缀树
    private class TrieNode{
        //关键词结束标识
        private boolean isKeywordEnd = false;
        // 子节点(key是下级字符,value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }
        public void setKeywordEnd(boolean keywordEnd){
            isKeywordEnd = keywordEnd;
        }
        //添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c,node);
        }
        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }

}
