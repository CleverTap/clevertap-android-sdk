package com.clevertap.demo.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.clevertap.demo.R

data class CtFeatureRowBinding(
    val title: TextView
)

data class CtFeatureFunctionsBinding(
    val fTitle: TextView
)
class HomeScreenListAdapter(
    private val viewModel: HomeScreenViewModel,
    private val titleList: List<String>,
    private val detailsList: Map<String, List<String>>
) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): Any = titleList[groupPosition]

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {

        val view = convertView
            ?: LayoutInflater.from(parent?.context).inflate(R.layout.ct_feature_row, parent, false).also { view ->
                val binding = CtFeatureRowBinding(
                    title = view.findViewById(R.id.featureTitle)
                )
                view.tag = binding
            }

        (view.tag as? CtFeatureRowBinding)?.title?.text = getGroup(groupPosition) as String

        return view
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

        val view = convertView
            ?: LayoutInflater.from(parent?.context).inflate(R.layout.ct_feature_functions, parent, false).also { view ->
                val binding = CtFeatureFunctionsBinding(
                    fTitle = view.findViewById(R.id.functionTitle)
                )
                view.tag = binding
            }
        (view.tag as? CtFeatureFunctionsBinding)?.fTitle?.apply {
            text = getChild(groupPosition, childPosition) as String
            setOnClickListener(null)
            setOnClickListener {
                viewModel.onChildClick(groupPosition = groupPosition, childPosition = childPosition)
            }
        }

        return view
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun getGroupCount(): Int = titleList.size
}