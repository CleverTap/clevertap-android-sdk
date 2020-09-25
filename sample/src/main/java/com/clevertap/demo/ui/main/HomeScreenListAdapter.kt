package com.clevertap.demo.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.clevertap.demo.databinding.CtFeatureFunctionsBinding
import com.clevertap.demo.databinding.CtFeatureRowBinding

class HomeScreenListAdapter(
    private val viewModel: HomeScreenViewModel, private val titleList: List<String>,
    private val detailsList: Map<String, List<String>>
) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): Any = titleList[groupPosition]

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var convertViewShadow = convertView
        val binding: CtFeatureRowBinding

        if (convertViewShadow == null) {
            val layoutInflater = LayoutInflater.from(parent?.context)
            binding = CtFeatureRowBinding.inflate(layoutInflater, parent, false)
            convertViewShadow = binding.root
        } else {
            binding = convertViewShadow.tag as CtFeatureRowBinding
        }

        binding.title = getGroup(groupPosition) as String

        convertViewShadow.tag = binding
        return convertViewShadow
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return detailsList[titleList[groupPosition]]?.size ?: 0
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return detailsList[titleList[groupPosition]]?.get(childPosition) ?: ""
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var convertViewShadow = convertView
        val binding: CtFeatureFunctionsBinding

        if (convertViewShadow == null) {
            val layoutInflater = LayoutInflater.from(parent?.context)
            binding = CtFeatureFunctionsBinding.inflate(layoutInflater, parent, false)
            convertViewShadow = binding.root
        } else {
            binding = convertViewShadow.tag as CtFeatureFunctionsBinding
        }

        binding.fTitle = getChild(groupPosition, childPosition) as String
        binding.groupPosition = groupPosition
        binding.childPosition = childPosition
        binding.viewmodel = viewModel

        convertViewShadow.tag = binding
        return convertViewShadow
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun getGroupCount(): Int = titleList.size
}